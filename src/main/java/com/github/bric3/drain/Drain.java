package com.github.bric3.drain;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Port of the Drain algorithm for log parsing
 *
 * This code comes from a modified work of the LogPai team by IBM engineers,
 * but it has been improved to fit the Java platform.
 *
 * Example use:
 * <pre><code>
 * var drain = Drain.drainBuilder()
 *                  .additionalDelimiters("_")
 *                  .depth(4)
 *                  .build()
 * Files.lines(Paths.get("build/resources/test/SSH.log"),
 *             StandardCharsets.UTF_8)
 *      .forEach(drain::parseLogMessage);
 *
 * // do something with clusters
 * drain.clusters();
 * </code></pre>
 *
 * @author brice.dutheil@gmail.com
 * @modifiedBy david.ohana@ibm.com, moshikh@il.ibm.com
 * @originalAuthor LogPAI team
 * @license MIT
 */
public class Drain {
    private static final String PARAM_MARKER = "<*>";
    public static final int ROOT_AND_LEAF_LEVELS = 2;

    /**
     * Depth of all leaf nodes.
     * <p>
     * These are the nodes that contain the log clusters.
     */
    final int depth;

    /**
     * Similarity threshold.
     */
    final double similarityThreshold;

    /**
     * Maximum number of child nodes per node
     */
    final int maxChildPerNode;

    /**
     * Delimiters to apply when splitting log messages into words.
     * <p>
     * In addition to whitespaces.
     */
    private final String additionalDelimiters;

    /**
     * All log clusters.
     */
    private final List<LogCluster> clusters = new ArrayList<>();

    private final Node root = new Node("(ROOT)", 0);

    private Drain(int depth,
                  double similarityThreshold,
                  int maxChildPerNode,
                  String additionalDelimiters) {
        this.depth = depth - ROOT_AND_LEAF_LEVELS;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.additionalDelimiters = additionalDelimiters;
    }

    /**
     * Parse log message.
     *
     * Classify the log message to a cluster.
     *
     * @param message The log message content
     */
    public void parseLogMessage(@Nonnull String message) {
        // sprint message by delimiter / whitespaces
        var contentTokens = tokenize(message);

        // Search the prefix tree
        var matchCluster = treeSearch(root, contentTokens);

        if (matchCluster == null) {
            // create cluster if it doesn't exists, using log content tokens as template tokens
            matchCluster = new LogCluster(contentTokens);
            clusters.add(matchCluster);
            addSeqToPrefixTree(root, matchCluster);
        } else {
            // add the log to an existing cluster
            // TODO move logic to LogCluster class
            var newTemplateTokens = getTemplate(contentTokens, matchCluster.tokens());
            if (!String.join(" ", newTemplateTokens).equals(String.join(" ", matchCluster.tokens()))) {
                matchCluster.updateTokens(newTemplateTokens);
            }
            matchCluster.newSighting();
        }
    }

    private List<String> tokenize(String content) {
        return Splitter.on(CharMatcher.anyOf(" " + additionalDelimiters))
                            .trimResults()
                            .omitEmptyStrings()
                            .splitToList(content);
    }

    @Nonnull
    List<String> getTemplate(@Nonnull List<String> contentTokens,
                             @Nonnull List<String> templateTokens) {
        assert contentTokens.size() == templateTokens.size();
        var newTemplate = new ArrayList<String>(contentTokens.size());

        for (int i = 0, tokensSize = contentTokens.size(); i < tokensSize; i++) {
            var contentToken = contentTokens.get(i);
            var templateToken = templateTokens.get(i);
            // TODO change to replace value at index
            if (contentToken.equals(templateToken)) {
                newTemplate.add(contentToken);
            } else {
                newTemplate.add(PARAM_MARKER); // replace contentToken by a marker
            }

        }

        return newTemplate;
    }


    private @Nullable
    LogCluster treeSearch(@Nonnull Node root,
                          @Nonnull List<String> logTokens) {

        // at first level, children are grouped by token (word) count
        var tokensCount = logTokens.size();
        var node = root.get(tokensCount);

        // the prefix tree is empty
        if (node == null) {
            return null;
        }

        // handle case of empty log string - return the single cluster in that group
        if (tokensCount == 0) {
            return node.clusterOf(0);
        }

        // find the leaf node for this log
        // a path of nodes matching the first N tokens (N=tree depth)
        int currentDepth = 1;
        for (String token : logTokens) {
            // if max depth reached, bail out
            if (currentDepth == this.depth) {
                break;
            }
            // if last parseable token, bail out
            if (currentDepth == tokensCount) {
                break;
            }
            // descend
            var nextNode = node.get(token);
            // if null try get from generic pattern
            if (nextNode == null) {
                nextNode = node.get(PARAM_MARKER);
            }
            // if the node don't exists yet, the cluster don't exists yet
            if (nextNode == null) {
                return null;
            }
            node = nextNode;
            currentDepth++;
        }

        return fastMatch(node.clusters(), logTokens);
    }

    private @Nullable
    LogCluster fastMatch(@Nonnull List<LogCluster> clusters,
                         @Nonnull List<String> logTokens) {
        LogCluster matchedCluster = null;

        double maxSimilarity = -1;
        int maxParamCount = -1;
        LogCluster maxCluster = null;

        for (LogCluster cluster : clusters) {
            var seqDistance = getSeqDistance(cluster.tokens(), logTokens);
            if (seqDistance.similarity > maxSimilarity
                || (seqDistance.similarity == maxSimilarity && seqDistance.paramCount > maxParamCount)) {
                maxSimilarity = seqDistance.similarity;
                maxParamCount = seqDistance.paramCount;
                maxCluster = cluster;
            }
        }

        if (maxSimilarity >= similarityThreshold) {
            matchedCluster = maxCluster;
        }

        return matchedCluster;
    }

    private static class SeqDistance {

        final double similarity;
        final int paramCount;
        SeqDistance(double similarity, int paramCount) {
            this.similarity = similarity;
            this.paramCount = paramCount;
        }

    }
    @Nonnull
    SeqDistance getSeqDistance(@Nonnull List<String> templateTokens,
                               @Nonnull List<String> logTokens) {
        assert templateTokens.size() == logTokens.size();

        int similarTokens = 0;
        int paramCount = 0;

        for (int i = 0, tokensSize = templateTokens.size(); i < tokensSize; i++) {
            String token = templateTokens.get(i);
            String currentToken = logTokens.get(i);

            if (token.equals(PARAM_MARKER)) {
                paramCount++;
                continue;
            }
            if (token.equals(currentToken)) {
                similarTokens++;
            }
        }

        double similarity = (double) similarTokens / templateTokens.size();
        return new SeqDistance(similarity, paramCount);
    }

    private void addSeqToPrefixTree(@Nonnull Node root,
                                    @Nonnull LogCluster newLogCluster) {
        int tokenCount = newLogCluster.tokens().size();

        var node = root.getOrCreateChild(tokenCount);

        // handle case of empty log message
        if (tokenCount == 0) {
            node.appendCluster(newLogCluster);
            return;
        }


        int currentDepth = 1;
        for (String token : newLogCluster.tokens()) {

            // Add current log cluster to the leaf node
            boolean atMaxDepth = currentDepth == depth;
            boolean isLastToken = currentDepth == tokenCount;
            if (atMaxDepth || isLastToken) {
                node.appendCluster(newLogCluster);
                break;
            }

            // If token not matched in this layer of existing tree.
            // TODO see improvements are possible
            if (!node.contains(token)) {
                if (!hasNumber(token)) {
                    if (node.contains(PARAM_MARKER)) {
                        if (node.childrenCount() < maxChildPerNode) {
                            node = node.getOrCreateChild(token);
                        } else {
                            node = node.get(PARAM_MARKER);
                        }
                    } else {
                        if (node.childrenCount() + 1 <= maxChildPerNode) {
                            node = node.getOrCreateChild(token);
                        } else if (node.childrenCount() + 1 == maxChildPerNode) {
                            node = node.getOrCreateChild(PARAM_MARKER);
                        } else {
                            node = node.get(PARAM_MARKER);
                        }
                    }
                } else {
                    if (!node.contains(PARAM_MARKER)) {
                        node = node.getOrCreateChild(PARAM_MARKER);
                    } else {
                        node = node.get(PARAM_MARKER);
                    }
                }
            } else {
                node = node.get(token);
            }
            currentDepth++;
        }
    }


    private static boolean hasNumber(@Nonnull String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    public List<LogCluster> clusters() {
        return List.copyOf(clusters);
    }


    /**
     * Drain builder.
     *
     * Used like this:
     * <pre><code>
     *     Drain.drainBuilder()
     *          .additionalDelimiters("_")
     *          .depth(4)
     *          .build()
     * </code></pre>
     * 
     * @return a drain builder
     */
    public static DrainBuilder drainBuilder() {
        return new DrainBuilder();
    }

    public static class DrainBuilder {
        private int depth = 4;
        private String additionalDelimiters = "";
        private double similarityThreshold = 0.4d;
        private int maxChildPerNode = 100;

        /**
         * Depth of all leaf nodes.
         *
         * How many level to reach the nodes that contain log clusters.
         *
         * THe default value is 4, the minimum value is 3.
         *
         * @param depth The depth of all leaf nodes.
         * @return this
         */
        public DrainBuilder depth(int depth) {
            assert depth > 2;
            this.depth = depth;
            return this;
        }

        /**
         * Additional delimiters.
         *
         * Additionally to the whitespace, also use additional delimiting
         * characters to to split the log message into tokens. This value
         * is empty by default.
         *
         * @param additionalDelimiters THe Additional delimiters.
         * @return this
         */
        public DrainBuilder additionalDelimiters(String additionalDelimiters) {
            assert additionalDelimiters != null;
            this.additionalDelimiters = additionalDelimiters;
            return this;
        }

        /**
         * Similarity threshold.
         *
         * The similarity threshold applies to each token of a log message,
         * if the percentage of similar tokens is below this number, then
         * a new log cluster will be created.
         *
         * Default value is 0.4.
         *
         * @param similarityThreshold  The similarity threshold
         * @return this
         */
        public DrainBuilder similarityThreshold(double similarityThreshold) {
            assert similarityThreshold > 0.1d;
            this.similarityThreshold = similarityThreshold;
            return this;
        }

        /**
         * Max number of children of an internal node.
         *
         * Limit the number of children nodes, if this value is too low
         * and log messages are too versatile then many logs will be
         * classified under the generic param marker.
         *
         * Default value is 100.
         *
         * @param maxChildPerNode Max number of children of an internal node
         * @return this
         */
        public DrainBuilder maxChildPerNode(int maxChildPerNode) {
            assert maxChildPerNode >= 2;
            this.maxChildPerNode = maxChildPerNode;
            return this;
        }

        /**
         * Build a non thread safe instance of Drain.
         *
         * @return A {@see Drain} instance
         */
        public Drain build() {
            return new Drain(depth,
                             similarityThreshold,
                             maxChildPerNode,
                             additionalDelimiters);
        }
    }
}

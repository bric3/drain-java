package com.github.bric3.drain;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Port of the Drain algorithm for log parsing
 * <p>
 * This code comes from a modified work of the LogPai team by IBM engineers.
 *
 * @author brice.dutheil@gmail.com
 * @modifiedBy david.ohana@ibm.com, moshikh@il.ibm.com
 * @originalAuthor LogPAI team
 * @license MIT
 */
public class Drain {
    private static final String param_str = "<*>";

    /**
     * Depth of all leaf nodes.
     * <p>
     * These are the nodes that contain the log clusters.
     */
    final int depth = 4;

    /**
     * Similarity threshold.
     */
    final double similarityThreshold = 0.4;

    /**
     * Maximum number of child nodes per node
     */
    final int maxChildren = 100;

    /**
     * Delimiters to apply when splitting log messages into words.
     * <p>
     * In addition to whitespaces.
     */
    private final String extraDelimiters;

    /**
     * All log clusters.
     */
    private final List<LogCluster> clusters = new ArrayList<>();

    final Node root = new Node("(ROOT)", 0);

    public Drain(String extraDelimiters) {
        this.extraDelimiters = extraDelimiters;
    }

    public void parseLogMessage(@Nonnull String content) {
        // sprint message by delimiter / whitespaces
        var contentTokens = tokenize(content);

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
        var contentTokens = Splitter.on(CharMatcher.anyOf(" " + extraDelimiters))
                            .trimResults()
                            .omitEmptyStrings()
                            .splitToList(content);


//        var m = extraDelimiters.matcher(content);
//        var sb = new StringBuilder();
//        while (m.find()) {
//            m.appendReplacement(sb, " ");
//        }
//        m.appendTail(sb);
//        content = sb.toString();
//        var contentTokens = content.split("\\s+");
        return contentTokens;
    }

    private @Nonnull
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
                newTemplate.add(param_str); // replace contentToken by a marker
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
                nextNode = node.get(param_str);
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

            if (token.equals(param_str)) {
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
                    if (node.contains(param_str)) {
                        if (node.childrenCount() < maxChildren) {
                            node = node.getOrCreateChild(token);
                        } else {
                            node = node.get(param_str);
                        }
                    } else {
                        if (node.childrenCount() + 1 <= maxChildren) {
                            node = node.getOrCreateChild(token);
                        } else if (node.childrenCount() + 1 == maxChildren) {
                            node = node.getOrCreateChild(param_str);
                        } else {
                            node = node.get(param_str);
                        }
                    }
                } else {
                    if (!node.contains(param_str)) {
                        node = node.getOrCreateChild(param_str);
                    } else {
                        node = node.get(param_str);
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

}

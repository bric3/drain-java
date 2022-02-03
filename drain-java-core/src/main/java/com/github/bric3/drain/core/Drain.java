/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain.core;

import com.github.bric3.drain.utils.Tokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Drain log pattern miner.
 *
 * This code comes from a modified work of the LogPai team by IBM engineers,
 * but it has been improved to fit the Java platform.
 *
 * Use the builder method {@link #drainBuilder()} to configure an
 * instance.
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
    /**
     * Marker for similar tokens
     */
    public static final String PARAM_MARKER = "<*>";
    private static final int ROOT_AND_LEAF_LEVELS = 2;

    /**
     * Depth of all leaf nodes.
     *
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
     *
     * In addition to whitespaces.
     */
    final String delimiters;

    /**
     * All log clusters.
     */
    private final List<InternalLogCluster> clusters;

    private final Node root;

    private Drain(int depth,
                  double similarityThreshold,
                  int maxChildPerNode,
                  String additionalDelimiters) {
        this.depth = depth - ROOT_AND_LEAF_LEVELS;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.delimiters = " " + additionalDelimiters;
        root = new Node("(ROOT)", 0);
        clusters = new ArrayList<>();
    }

    Drain(DrainState state) {
        this.depth = state.depth;
        this.similarityThreshold = state.similarityThreshold;
        this.maxChildPerNode = state.maxChildPerNode;
        this.delimiters = state.delimiters;
        this.clusters = state.clusters;
        this.root = state.prefixTree;
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
        var contentTokens = Tokenizer.tokenize(message, delimiters);

        // Search the prefix tree
        var matchCluster = treeSearch(contentTokens);

        if (matchCluster == null) {
            // create cluster if it doesn't exists, using log content tokens as template tokens
            matchCluster = new InternalLogCluster(contentTokens);
            clusters.add(matchCluster);
            addLogClusterToPrefixTree(matchCluster);
        } else {
            // add the log to an existing cluster
            matchCluster.newSighting(contentTokens);
        }
    }

    private @Nullable
    InternalLogCluster treeSearch(@Nonnull List<String> logTokens) {

        // at first level, children are grouped by token (word) count
        var tokensCount = logTokens.size();
        var node = this.root.get(tokensCount);

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
            // if max depth reached or last parseable token, bail out
            boolean atMaxDepth = currentDepth == this.depth;
            boolean isLastToken = currentDepth == tokensCount;
            if (atMaxDepth || isLastToken) {
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
    InternalLogCluster fastMatch(@Nonnull List<InternalLogCluster> clusters,
                                 @Nonnull List<String> logTokens) {
        InternalLogCluster matchedCluster = null;

        double maxSimilarity = -1;
        int maxParamCount = -1;
        InternalLogCluster maxCluster = null;

        for (InternalLogCluster cluster : clusters) {
            var seqDistance = computeSeqDistance(cluster.internalTokens(), logTokens);
            if (seqDistance.similarity > maxSimilarity
                || (seqDistance.similarity == maxSimilarity
                    && seqDistance.paramCount > maxParamCount)) {
                maxSimilarity = seqDistance.similarity;
                maxParamCount = seqDistance.paramCount;
                maxCluster = cluster;
            }
        }

        if (maxSimilarity >= this.similarityThreshold) {
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

    static @Nonnull
    SeqDistance computeSeqDistance(@Nonnull List<String> templateTokens,
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

    private void addLogClusterToPrefixTree(@Nonnull InternalLogCluster newLogCluster) {
        int tokensCount = newLogCluster.internalTokens().size();

        var node = this.root.getOrCreateChild(tokensCount);

        // handle case of empty log message
        if (tokensCount == 0) {
            node.appendCluster(newLogCluster);
            return;
        }


        int currentDepth = 1;
        for (String token : newLogCluster.internalTokens()) {

            // Add current log cluster to the leaf node
            boolean atMaxDepth = currentDepth == this.depth;
            boolean isLastToken = currentDepth == tokensCount;
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

    /**
     * Returns a list of the Log clusters.
     *
     * @return Non modifiable list of current clusters.
     */
    public List<LogCluster> clusters() {
        return List.copyOf(clusters);
    }

    Node prefixTree() {
        return root;
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
         * The default value is 4, the minimum value is 3.
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

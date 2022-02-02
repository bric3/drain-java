/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Drain Jackson Serializer
 *
 */
public class DrainJson {
    /**
     * Marker for similar tokens
     */
    public static final String PARAM_MARKER = "<*>";
    private static final int ROOT_AND_LEAF_LEVELS = 2;

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
     *
     * In addition to whitespaces.
     */
    private final String delimiters;

    /**
     * All log clusters.
     */
    private final List<LogCluster> clusters;

    private final Node root;

    private DrainJson(int depth,
                      double similarityThreshold,
                      int maxChildPerNode,
                      String additionalDelimiters) {
        this.depth = depth - ROOT_AND_LEAF_LEVELS;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.delimiters = " " + additionalDelimiters;
        this.clusters = new ArrayList<>();
        this.root = new Node("(ROOT)", 0);
    }

    /**
     * Json creator and Json properties allowing Drain-object import.
     */
    @JsonCreator
    private DrainJson(@JsonProperty("depth") int depth,
                      @JsonProperty("similarityThreshold") double similarityThreshold,
                      @JsonProperty("maxChildPerNode") int maxChildPerNode,
                      @JsonProperty("delimiters") String additionalDelimiters,
                      @JsonProperty("clusters") List<LogCluster> clusters,
                      @JsonProperty("root") Node root) {
        this.depth = depth;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.delimiters = " " + additionalDelimiters;
        this.clusters = new ArrayList<LogCluster>();
        this.root = root;
//        logClustersLinker(this.root, this.clusters);
    }

    /**
     * Links the list of clusters from tree nodes to Drain.clusters
     * (needed when importing a model)
     */
//    private void logClustersLinker(Node aux, List<LogCluster> clusters) {
//        if (aux != null) {
//            for (Node n: aux.allChildren().values()) {
//                if (!n.clusters().isEmpty()) {
//                    clusters.addAll(n.clusters());
//                }
//                logClustersLinker(n, clusters);
//            }
//        }
//    }

    /**
     * Drain-object exporting functionality which saves a drain model
     * in a json file at given path.
     */
    public void drainExport(String filePath) {
        // Instance an Object Mapper and allow the access to the Drain object attributes
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        
        // Write the Drain object attributes into path file
        try {
            System.out.printf("---- Saving drain model in file %s %n",
                              filePath);
            objectMapper.writeValue(new FileOutputStream(filePath), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Drain-object importing functionality which creates a Drain instance from the input
     * json filepath containing Drain object attributes.
     */
    public static DrainJson drainImport(String filePath) throws IOException {
        // Create an Object mapper
        ObjectMapper objectMapper = new ObjectMapper();

        // Setup the input .json file
        File inputDrain = new File(filePath);

        System.out.printf("---- Recovering drain model from file %s %n",
                          filePath);
        // Map the attributes from the file and return the Drain instance
        return objectMapper.readValue(inputDrain, DrainJson.class);
    }

    /**
     * Returns the matching log cluster given a log message (null if no matchings)
     *
     */
    public LogCluster findLogMessage(@Nonnull String message) {
        // sprint message by delimiter / whitespaces
        var contentTokens = tokenize(message);

        // Search the prefix tree
        var matchCluster = treeSearch(contentTokens);

        //System.out.println("Found node: "+matchCluster);

        return matchCluster;
    }

    /**
     * Parse log message.
     *
     * Classify the log message to a cluster.
     *
     * @param message The log message content
     */
    public LogCluster parseLogMessage(@Nonnull String message) {
        // sprint message by delimiter / whitespaces
        var contentTokens = tokenize(message);

        // Search the prefix tree
        var matchCluster = treeSearch(contentTokens);

        if (matchCluster == null) {
            // create cluster if it doesn't exists, using log content tokens as template tokens
            matchCluster = new LogCluster(contentTokens);
            clusters.add(matchCluster);
            addLogClusterToPrefixTree(matchCluster);
        } else {
            // add the log to an existing cluster
            matchCluster.newSighting(contentTokens);
        }
        //System.out.println("Found node: "+matchCluster);

        return matchCluster;
    }

    private List<String> tokenize(String content) {
        return Splitter.on(CharMatcher.anyOf(delimiters))
                            .trimResults()
                            .omitEmptyStrings()
                            .splitToList(content);
    }

    private @Nullable
    LogCluster treeSearch(@Nonnull List<String> logTokens) {

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
    LogCluster fastMatch(@Nonnull List<LogCluster> clusters,
                         @Nonnull List<String> logTokens) {
        LogCluster matchedCluster = null;

        double maxSimilarity = -1;
        int maxParamCount = -1;
        LogCluster maxCluster = null;

        for (LogCluster cluster : clusters) {
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

    private void addLogClusterToPrefixTree(@Nonnull LogCluster newLogCluster) {
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
        public DrainJson build() {
            return new DrainJson(depth,
                                 similarityThreshold,
                                 maxChildPerNode,
                                 additionalDelimiters);
        }
    }
}

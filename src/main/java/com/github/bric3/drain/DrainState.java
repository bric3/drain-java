package com.github.bric3.drain;

import java.util.List;

class DrainState {
    final int depth;
    final double similarityThreshold;
    final int maxChildPerNode;
    final String delimiters;
    final List<LogCluster> clusters;
    final Node prefixTree;

    DrainState(int depth,
               double similarityThreshold,
               int maxChildPerNode,
               String delimiters,
               List<LogCluster> clusters,
               Node prefixTree) {
        this.depth = depth;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.delimiters = delimiters;
        this.clusters = clusters;
        this.prefixTree = prefixTree;
    }
}

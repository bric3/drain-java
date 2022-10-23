/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.drain.core;

import java.util.List;

class DrainState {
    final int depth;
    final double similarityThreshold;
    final int maxChildPerNode;
    final String delimiters;
    final List<InternalLogCluster> clusters;
    final Node prefixTree;

    DrainState(int depth,
               double similarityThreshold,
               int maxChildPerNode,
               String delimiters,
               List<InternalLogCluster> clusters,
               Node prefixTree) {
        this.depth = depth;
        this.similarityThreshold = similarityThreshold;
        this.maxChildPerNode = maxChildPerNode;
        this.delimiters = delimiters;
        this.clusters = clusters;
        this.prefixTree = prefixTree;
    }
}

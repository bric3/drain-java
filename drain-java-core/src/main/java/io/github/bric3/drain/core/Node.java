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

import java.util.*;

class Node {
    final int depth;
    final Object key;
    private final HashMap<Object, Node> keyToChildNode;
    private final List<InternalLogCluster> clusters;

    public Node(Object key, int depth) {
        this.key = key;
        this.depth = depth;
        this.keyToChildNode = new HashMap<>();
        this.clusters = new ArrayList<>();
    }

    Node(Object key, int depth, HashMap<Object, Node> keyToChildNode, List<InternalLogCluster> clusters) {
        this.depth = depth;
        this.key = key;
        this.keyToChildNode = keyToChildNode;
        this.clusters = clusters;
    }

    public Node get(Object key) {
        return keyToChildNode.get(key);
    }

    public <T extends String> Node getOrCreateChild(Object key) {
        return keyToChildNode.computeIfAbsent(
                key,
                k -> new Node(k, depth + 1)
        );
    }

    InternalLogCluster clusterOf(int tokenCount) {
        return clusters.get(tokenCount);
    }

    List<InternalLogCluster> clusters() {
        return clusters;
    }

    void appendCluster(InternalLogCluster cluster) {
        clusters.add(cluster);
    }

    public boolean contains(String key) {
        return keyToChildNode.containsKey(key);
    }

    public int childrenCount() {
        return keyToChildNode.size();
    }

    Map<Object, Node> childMappings() {
        return Collections.unmodifiableMap(new HashMap<>(keyToChildNode));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return depth == node.depth && Objects.equals(key, node.key) && Objects.equals(keyToChildNode, node.keyToChildNode) && Objects.equals(clusters, node.clusters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depth, key, keyToChildNode, clusters);
    }

    @Override
    public String toString() {
        return "Node{" +
               "depth=" + depth +
               ", key=" + key +
               ", keyToChildNode=" + keyToChildNode +
               ", clusters=" + clusters +
               '}';
    }
}

package com.github.bric3.drain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class Node {
    final int depth;
    final Object key;
    private final HashMap<Object, Node> keyToChildNode;
    private final List<LogCluster> clusters;

    public Node(Object key, int depth) {
        this.key = key;
        this.depth = depth;
        this.keyToChildNode = new HashMap<>();
        this.clusters = new ArrayList<>();
    }

    Node(Object key, int depth, HashMap<Object, Node> keyToChildNode, List<LogCluster> clusters) {
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

    public LogCluster clusterOf(int tokenCount) {
        return clusters.get(tokenCount);
    }

    public List<LogCluster> clusters() {
        return clusters;
    }

    public void appendCluster(LogCluster cluster) {
        clusters.add(cluster);
    }

    public boolean contains(String key) {
        return keyToChildNode.containsKey(key);
    }

    public int childrenCount() {
        return keyToChildNode.size();
    }

    Map<Object, Node> childMappings() {
        return Map.copyOf(keyToChildNode);
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

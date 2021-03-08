package com.github.bric3.drain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Node {
    private final int depth;
    private final Object key;
    private final HashMap<Object, Node> keyToChildNode;
    private final List<LogCluster> clusters;

    public Node(Object key, int depth) {
        this.key = key;
        this.depth = depth;
        this.keyToChildNode = new HashMap<>();
        this.clusters= new ArrayList<>();
    }

    /**
     * EDIT: Todor Krasimirov from DEVO
     *
     * Json creator and Json properties added allowing Drain-object import.
     */
    @JsonCreator
    public Node(@JsonProperty("depth") int depth,
                @JsonProperty("key") Object key,
                @JsonProperty("keyToChildNode") HashMap<Object, Node> keyToChildNode,
                @JsonProperty("clusters") List<LogCluster> clusters) {
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
}

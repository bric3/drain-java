package com.github.bric3.drain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Node {
    private final int depth;
    private final Object key;
    private final HashMap<Object, Node> keyToChildNode = new HashMap<>();
    private final List<LogCluster> clusters = new ArrayList<>();

    public Node(Object key, int depth) {
        this.key = key;
        this.depth = depth;
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

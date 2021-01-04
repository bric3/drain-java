package com.github.bric3.drain;

import javax.annotation.Nonnull;
import java.util.List;

class LogCluster {
    private static int instanceCounter = 0;

    private final int clusterId;
    private int size = 1;
    private List<String> logTemplateTokens;

    public LogCluster(@Nonnull List<String> logTemplateTokens) {
        this.clusterId = ++instanceCounter;
        this.logTemplateTokens = logTemplateTokens;
    }

    public List<String> tokens() {
        return logTemplateTokens;
    }

    public void updateTokens(List<String> newTemplateTokens) {
        logTemplateTokens = newTemplateTokens;
    }

    public void newSighting() {
        size++;
    }

    public int sightings() {
        return size;
    }

    @Override
    public String toString() {
        return String.format("%04d (size=%d): %s",
                             clusterId,
                             size,
                             String.join(" ", logTemplateTokens));
    }
}

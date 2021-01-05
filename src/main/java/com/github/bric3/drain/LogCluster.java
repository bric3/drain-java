package com.github.bric3.drain;

import javax.annotation.Nonnull;
import java.util.List;

public class LogCluster {
    private static int instanceCounter = 0;

    private final int clusterId;
    private int sightings = 1;
    private List<String> logTemplateTokens;

    LogCluster(@Nonnull List<String> logTemplateTokens) {
        this.clusterId = ++instanceCounter;
        this.logTemplateTokens = logTemplateTokens;
    }

    public List<String> tokens() {
        return logTemplateTokens;
    }

    void updateTokens(List<String> newTemplateTokens) {
        logTemplateTokens = newTemplateTokens;
    }

    void newSighting() {
        sightings++;
    }

    public int sightings() {
        return sightings;
    }

    @Override
    public String toString() {
        return String.format("%04d (size=%d): %s",
                             clusterId,
                             sightings,
                             String.join(" ", logTemplateTokens));
    }
}

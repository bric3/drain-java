package com.github.bric3.drain;

import javax.annotation.Nonnull;
import java.util.ArrayList;
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

    void newSighting(List<String> contentTokens) {
        var newTemplateTokens = updateTemplate(contentTokens, logTemplateTokens);
        if (!String.join(" ", newTemplateTokens).equals(String.join(" ", logTemplateTokens))) {
            updateTokens(newTemplateTokens);
        }

        sightings++;
    }

    @Nonnull
    List<String> updateTemplate(@Nonnull List<String> contentTokens,
                                @Nonnull List<String> templateTokens) {
        assert contentTokens.size() == templateTokens.size();
        var newTemplate = new ArrayList<String>(contentTokens.size());

        for (int i = 0, tokensSize = contentTokens.size(); i < tokensSize; i++) {
            var contentToken = contentTokens.get(i);
            var templateToken = templateTokens.get(i);
            // TODO change to replace value at index
            if (contentToken.equals(templateToken)) {
                newTemplate.add(contentToken);
            } else {
                newTemplate.add(Drain.PARAM_MARKER); // replace contentToken by a marker
            }

        }

        return newTemplate;
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

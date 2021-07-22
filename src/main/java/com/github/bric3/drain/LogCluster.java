package com.github.bric3.drain;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Log cluster.
 * <p>
 * It represents a tokenized logs where similar tokens are
 * replaced by the marker {@link Drain#PARAM_MARKER}.
 *
 * @author brice.dutheil@gmail.com
 * @modifiedBy david.ohana@ibm.com, moshikh@il.ibm.com
 * @originalAuthor LogPAI team
 * @license MIT
 */
public class LogCluster {
    private final UUID clusterId;
    private int sightings = 1;
    private List<String> logTemplateTokens;

    LogCluster(@Nonnull List<String> logTemplateTokens) {
        this.clusterId = UUID.randomUUID();
        this.logTemplateTokens = logTemplateTokens;
    }

    // for deserialization
    private LogCluster(UUID clusterId,
                       int sightings,
                       List<String> logTemplateTokens) {
        this.clusterId = clusterId;
        this.sightings = sightings;
        this.logTemplateTokens = logTemplateTokens;
    }

    /**
     * The cluster identifier
     *
     * @return cluster identifier.
     */
    public UUID clusterId() {
        return clusterId;
    }

    List<String> internalTokens() {
        return logTemplateTokens;
    }

    /**
     * List of the tokens for this LogCLuster
     *
     * @return Tokens of this cluster
     */
    public List<String> tokens() {
        return logTemplateTokens;
    }

    void updateTokens(List<String> newTemplateTokens) {
        logTemplateTokens = newTemplateTokens;
    }

    void newSighting(List<String> contentTokens) {
        List<String> newTemplateTokens = updateTemplate(contentTokens, logTemplateTokens);
        if (!newTemplateTokens.equals(logTemplateTokens)) {
            updateTokens(newTemplateTokens);
        }

        sightings++;
    }

    @Nonnull
    List<String> updateTemplate(@Nonnull List<String> contentTokens,
                                @Nonnull List<String> templateTokens) {
        assert contentTokens.size() == templateTokens.size();
        List<String> newTemplate = new ArrayList<String>(contentTokens.size());

        for (int i = 0, tokensSize = contentTokens.size(); i < tokensSize; i++) {
            String contentToken = contentTokens.get(i);
            String templateToken = templateTokens.get(i);
            // TODO change to replace value at index
            if (contentToken.equals(templateToken)) {
                newTemplate.add(contentToken);
            } else {
                newTemplate.add(Drain.PARAM_MARKER); // replace contentToken by a marker
            }

        }

        return newTemplate;
    }

    /**
     * The number of times a log with this pattern has been seen.
     *
     * @return sightings of similar logs.
     */
    public int sightings() {
        return sightings;
    }

    @Override
    public String toString() {
        return String.format("%s (size %d): %s",
                             clusterId,
                             sightings,
                             String.join(" ", logTemplateTokens));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogCluster that = (LogCluster) o;
        return sightings == that.sightings && clusterId.equals(that.clusterId) && logTemplateTokens.equals(that.logTemplateTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, sightings, logTemplateTokens);
    }
}

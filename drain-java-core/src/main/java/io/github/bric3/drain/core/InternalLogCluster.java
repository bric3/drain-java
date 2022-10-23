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


import javax.annotation.Nonnull;
import java.util.*;

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
class InternalLogCluster implements LogCluster {
    private final UUID clusterId;
    private int sightings = 1;
    private List<String> logTemplateTokens;

    InternalLogCluster(@Nonnull List<String> logTemplateTokens) {
        this.clusterId = UUID.randomUUID();
        this.logTemplateTokens = logTemplateTokens;
    }

    // for deserialization
    private InternalLogCluster(UUID clusterId,
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
    @Override
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
    @Override
    public List<String> tokens() {
        return Collections.unmodifiableList(logTemplateTokens);
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
    @Override
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
        InternalLogCluster that = (InternalLogCluster) o;
        return sightings == that.sightings && clusterId.equals(that.clusterId) && logTemplateTokens.equals(that.logTemplateTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, sightings, logTemplateTokens);
    }
}

package io.github.bric3.drain.core;

import java.util.List;
import java.util.UUID;

/**
 * Represents a cluster of log message parts.
 *
 * <p>A cluster consists of a list of tokens, one of the token might be the wild card {@link Drain#PARAM_MARKER &lt;*&gt;}.
 */
public interface LogCluster {
    /**
     * @return the cluster identifier.
     */
    UUID clusterId();

    /**
     * @return the list of tokens.
     */
    List<String> tokens();

    /**
     * @return the number similar log messages have been seen by this cluster.
     */
    int sightings();
}

package com.github.bric3.drain.core;

import java.util.List;
import java.util.UUID;

public interface LogCluster {
    UUID clusterId();

    List<String> tokens();

    int sightings();
}

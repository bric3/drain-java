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

import io.github.bric3.drain.utils.TestPaths;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class DrainJsonSerializationTest {

    @Test
    void serde_should_result_in_same_state() throws IOException {
        Drain drain = initDrain("SSH.log", l -> l.substring(l.indexOf("]: ") + 3));
        assertThat(drain.clusters()).hasSize(51);

        final Drain drainReloaded = serde(drain);

        assertThat(drainReloaded.depth).isEqualTo(drain.depth);
        assertThat(drainReloaded.similarityThreshold).isEqualTo(drain.similarityThreshold);
        assertThat(drainReloaded.delimiters).isEqualTo(drain.delimiters);
        assertThat(drainReloaded.maxChildPerNode).isEqualTo(drain.maxChildPerNode);
        assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());
        assertThat(drainReloaded.prefixTree()).isEqualTo(drain.prefixTree());
    }

    @Test
    void serde2_should_result_in_same_state() {
        Drain drain = Drain.drainBuilder()
                           .additionalDelimiters("_")
                           .depth(4)
                           .build();

        Stream.of("sent 550 bytes",
                  "sent 110 bytes",
                  "sent 800 bytes",
                  "received 1000 bytes",
                  "received 250 bytes",
                  "sent 200 bytes"
        ).forEach(drain::parseLogMessage);
        assertThat(drain.clusters()).hasSize(2);


        final Drain drainReloaded = serde(drain);

        assertThat(drainReloaded.depth).isEqualTo(drain.depth);
        assertThat(drainReloaded.similarityThreshold).isEqualTo(drain.similarityThreshold);
        assertThat(drainReloaded.delimiters).isEqualTo(drain.delimiters);
        assertThat(drainReloaded.maxChildPerNode).isEqualTo(drain.maxChildPerNode);
        assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());
        assertThat(drainReloaded.prefixTree()).isEqualTo(drain.prefixTree());
    }

    @Test
    void drain_with_reloaded_state_can_resume_log_mining() throws IOException {
        Drain drain = initDrain("Unity.log", l -> l.substring(l.indexOf(": ") + 2));

        Drain drainReloaded = serde(drain);
        assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());

        // Adding log with same patterns should not create new clusters
        Stream.of(
                "Sending Packet:{\"Type\":-1}",
                "Received ServerInfo packet:{\"Type\":2,\"PlayersOnline\":17,\"PlayersPlaying\":12,\"PublicCustomGames\":2,\"PlayersSearching\":2,\"MaintenanceIn\":-1}",
                "Received ServerInfo packet:{\"Type\":2,\"PlayersOnline\":17,\"PlayersPlaying\":12,\"PublicCustomGames\":2,\"PlayersSearching\":2,\"MaintenanceIn\":-1}",
                "Received KeepAlive packet:{\"Type\":-1}"
        ).forEach(drainReloaded::parseLogMessage);
        assertThat(drainReloaded.clusters()).hasSize(drain.clusters().size());

        // Adding a log with a different pattern should create a new cluster
        drainReloaded.parseLogMessage("Resolution changed: 2590x1600 windowed, Metal RecreateSurface[0x7ff491c541a0]: surface size 2588x1600");
        assertThat(drainReloaded.clusters()).hasSize(drain.clusters().size() + 1);
    }

    private Drain initDrain(String logFile, Function<String, String> normalizingFunction) throws IOException {
        Drain drain = Drain.drainBuilder()
                           .additionalDelimiters("_")
                           .depth(4)
                           .build();

        try (Stream<String> lines = Files.lines(TestPaths.get(logFile), UTF_8)) {
            lines.map(normalizingFunction)
                 .forEach(drain::parseLogMessage);
        }
        return drain;
    }

    private Drain serde(Drain drain) {
        final DrainJsonSerialization serde = new DrainJsonSerialization();
        final StringWriter writer = new StringWriter();
        serde.saveState(drain, writer);

        return serde.loadState(new StringReader(writer.toString()));
    }
}
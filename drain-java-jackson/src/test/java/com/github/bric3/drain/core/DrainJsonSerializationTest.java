/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain.core;

import com.github.bric3.drain.utils.TestPaths;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

class DrainJsonSerializationTest {

    @Test
    void serde_should_result_in_same_state() throws IOException {
        Drain drain = initDrain("SSH.log", l -> l.substring(l.indexOf("]: ") + 3));
        Assertions.assertThat(drain.clusters()).hasSize(51);

        final Drain drainReloaded = serde(drain);

        Assertions.assertThat(drainReloaded.depth).isEqualTo(drain.depth);
        Assertions.assertThat(drainReloaded.similarityThreshold).isEqualTo(drain.similarityThreshold);
        Assertions.assertThat(drainReloaded.delimiters).isEqualTo(drain.delimiters);
        Assertions.assertThat(drainReloaded.maxChildPerNode).isEqualTo(drain.maxChildPerNode);
        Assertions.assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());
        Assertions.assertThat(drainReloaded.prefixTree()).isEqualTo(drain.prefixTree());
    }

    @Test
    void serde2_should_result_in_same_state() throws IOException {
        var drain = Drain.drainBuilder()
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
        Assertions.assertThat(drain.clusters()).hasSize(2);


        final Drain drainReloaded = serde(drain);

        Assertions.assertThat(drainReloaded.depth).isEqualTo(drain.depth);
        Assertions.assertThat(drainReloaded.similarityThreshold).isEqualTo(drain.similarityThreshold);
        Assertions.assertThat(drainReloaded.delimiters).isEqualTo(drain.delimiters);
        Assertions.assertThat(drainReloaded.maxChildPerNode).isEqualTo(drain.maxChildPerNode);
        Assertions.assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());
        Assertions.assertThat(drainReloaded.prefixTree()).isEqualTo(drain.prefixTree());
    }

    @Test
    void drain_with_reloaded_state_can_resume_log_mining() throws IOException {
        var drain = initDrain("Unity.log", l -> l.substring(l.indexOf(": ") + 2));

        Drain drainReloaded = serde(drain);
        Assertions.assertThat(drainReloaded.clusters()).isEqualTo(drain.clusters());

        // Adding log with same patterns should not create new clusters
        Stream.of(
                "Sending Packet:{\"Type\":-1}",
                "Received ServerInfo packet:{\"Type\":2,\"PlayersOnline\":17,\"PlayersPlaying\":12,\"PublicCustomGames\":2,\"PlayersSearching\":2,\"MaintenanceIn\":-1}",
                "Received ServerInfo packet:{\"Type\":2,\"PlayersOnline\":17,\"PlayersPlaying\":12,\"PublicCustomGames\":2,\"PlayersSearching\":2,\"MaintenanceIn\":-1}",
                "Received KeepAlive packet:{\"Type\":-1}"
        ).forEach(drainReloaded::parseLogMessage);
        Assertions.assertThat(drainReloaded.clusters()).hasSize(drain.clusters().size());

        // Adding a log with a different pattern should create a new cluster
        drainReloaded.parseLogMessage("Resolution changed: 2590x1600 windowed, Metal RecreateSurface[0x7ff491c541a0]: surface size 2588x1600");
        Assertions.assertThat(drainReloaded.clusters()).hasSize(drain.clusters().size() + 1);
    }

    private Drain initDrain(String logFile, Function<String, String> normalizingFunction) throws IOException {
        var drain = Drain.drainBuilder()
                         .additionalDelimiters("_")
                         .depth(4)
                         .build();

        Files.lines(TestPaths.get(logFile), UTF_8)
             .map(normalizingFunction)
             .forEach(drain::parseLogMessage);
        return drain;
    }

    private Drain serde(Drain drain) throws IOException {
        final var serde = new DrainJsonSerialization();
        final var writer = new StringWriter();
        serde.saveState(drain, writer);

        return serde.loadState(new StringReader(writer.toString()));
    }
}
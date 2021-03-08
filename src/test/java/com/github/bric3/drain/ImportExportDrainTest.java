package com.github.bric3.drain;

import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ImportExportDrainTest {

    @Test
    void drainExportSSHTest() throws IOException {
        var drain = Drain.drainBuilder()
                         .additionalDelimiters("_")
                         .depth(4)
                         .build();

        var lineCounter = new AtomicInteger();

        var stopwatch = Stopwatch.createStarted();
        Files.lines(Paths.get("build/resources/test/SSH.log"),
                    StandardCharsets.UTF_8)
             .peek(__ -> lineCounter.incrementAndGet())
             .map(l -> l.substring(l.indexOf("]: ") + 3)) // removes this part: "Dec 10 06:55:46 LabSZ sshd[24200]: "
             .forEach(content -> {
                 drain.parseLogMessage(content);
                 if (lineCounter.get() % 10000 == 0) {
                     System.out.printf("%4d clusters so far%n", drain.clusters().size());
                 }
             });


        System.out.printf("---- Done processing file. Total of %d lines, done in %s, %d clusters%n",
                          lineCounter.get(),
                          stopwatch,
                          drain.clusters().size());
        drain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);

        assertThat(drain.clusters()).hasSize(51);
        drain.drainExport("DrainModels/SSH_Drain.json");
    }

    @Test
    void drainImportSSHTest() throws IOException {
        var drainPath = "DrainModels/SSH_Drain.json";
        var importedDrain = Drain.drainImport(drainPath);

        importedDrain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);

        assertThat(importedDrain.clusters()).hasSize(51);

    }

    @Test
    void drainExportSendRecTest() throws IOException {
        var drain = Drain.drainBuilder()
                         .additionalDelimiters("_")
                         .depth(4)
                         .build();

        var lineCounter = new AtomicInteger();

        var stopwatch = Stopwatch.createStarted();
        Files.lines(Paths.get("src/test/resources/SendRec.txt"),
                    StandardCharsets.UTF_8)
             .peek(__ -> lineCounter.incrementAndGet())
             .forEach(content -> {
                 drain.parseLogMessage(content);
                 System.out.printf("%4d clusters so far%n", drain.clusters().size());
             });


        System.out.printf("---- Done processing file. Total of %d lines, done in %s, %d clusters%n",
                          lineCounter.get(),
                          stopwatch,
                          drain.clusters().size());
        drain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);

        assertThat(drain.clusters()).hasSize(2);
        drain.drainExport("DrainModels/SendRec_Drain.json");
    }

    @Test
    void drainImportSendRecTest() throws IOException {
        var drainPath = "DrainModels/SendRec_Drain.json";
        var importedDrain = Drain.drainImport(drainPath);

        importedDrain.clusters()
                     .stream()
                     .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                     .forEach(System.out::println);

        assertThat(importedDrain.clusters()).hasSize(2);

    }

}
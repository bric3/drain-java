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

    @Test
    void findPatternFromImportedSendRecDrainTest() throws IOException {
        var drainPath = "DrainModels/SendRec_Drain.json";
        var importedDrain = Drain.drainImport(drainPath);

        importedDrain.clusters()
                     .stream()
                     .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                     .forEach(System.out::println);

        var logMessage = "sent 277 bytes";
        // use findLogMessage function to search the cluster belonging of a given log
        var matchCluster = importedDrain.findLogMessage(logMessage);

        System.out.println("\nMatching log cluster for \""+logMessage+"\": "+
                           matchCluster);

        assertThat(matchCluster.clusterId()).isEqualTo(1);

        logMessage = "no log pattern";
        matchCluster = importedDrain.findLogMessage(logMessage);

        System.out.println("Matching log cluster for \""+logMessage+"\": "+
                           matchCluster);

        assertThat(matchCluster).isEqualTo(null);
    }

    @Test
    void findPatternFromImportedSSHDrainTest() throws IOException {
        var drainPath = "DrainModels/SSH_Drain.json";
        var importedDrain = Drain.drainImport(drainPath);

        importedDrain.clusters()
                     .stream()
                     .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                     .forEach(System.out::println);

        var logMessage = "Connection closed by 168.124.112.4 [preauth]";
        // use findLogMessage function to search the cluster belonging of a given log
        var matchCluster = importedDrain.findLogMessage(logMessage);

        System.out.println("\nMatching log cluster for \""+logMessage+"\": "+
                           matchCluster);

        assertThat(matchCluster.clusterId()).isEqualTo(7);
    }

    @Test
    void updateImportedSendRecDrainTest() throws IOException {
        // Use parseLogMessage function for drain model updating with a new log message
        var drainPath = "DrainModels/SendRec_Drain.json";
        var importedDrain = Drain.drainImport(drainPath);

        System.out.println("Log clusters: ");
        importedDrain.clusters()
                     .stream()
                     .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                     .forEach(System.out::println);


        // 1. New log message with an existing log cluster
        var logMessage = "sent 777 bytes";
        // Classify the new log
        var matchCluster = importedDrain.parseLogMessage(logMessage);
        System.out.println("\nLog cluster for \""+logMessage+"\": "+
                           matchCluster);

        // After classifying there are 5 pattern sightings
        assertThat(matchCluster.sightings()).isEqualTo(5);


        // 2. New log message without a related log cluster
        logMessage = "couldn't send the package 23";
        matchCluster = importedDrain.parseLogMessage(logMessage);
        System.out.println("Log cluster for \""+logMessage+"\": "+
                           matchCluster);

        // Only 1 sighting of the new log cluster
        assertThat(matchCluster.sightings()).isEqualTo(1);


        // 3. New log message related to the new log cluster
        logMessage = "couldn't send the package 26";
        matchCluster = importedDrain.parseLogMessage(logMessage);
        System.out.println("Log cluster for \""+logMessage+"\": "+
                           matchCluster);

        // The sightings turns to 2
        assertThat(matchCluster.sightings()).isEqualTo(2);


        System.out.println("\nUpdated log clusters:");
        importedDrain.clusters()
                     .stream()
                     .sorted(Comparator.comparing(LogCluster::sightings).reversed())
                     .forEach(System.out::println);

        // Save the updated drain model
        importedDrain.drainExport("DrainModels/SendRec_Updated_Drain.json");
    }

}
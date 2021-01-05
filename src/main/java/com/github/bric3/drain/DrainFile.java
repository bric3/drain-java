package com.github.bric3.drain;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public class DrainFile {

    private final String stripUpTo;
    private final boolean verbose;

    public DrainFile(boolean verbose) {
        this.verbose = verbose;
        stripUpTo = "";
    }

    public void drain(Path file) {
        var drain = Drain.drainBuilder()
                         .additionalDelimiters("_")
                         .depth(4)
                         .build();

        var lineCounter = new AtomicInteger();

        var stopwatch = Stopwatch.createStarted();
        try {
            Files.lines(file, StandardCharsets.UTF_8)
                 .peek(__ -> lineCounter.incrementAndGet())
                 .map(l -> {
                     if (!stripUpTo.isEmpty()) {
                         return l.substring(l.indexOf(stripUpTo) + stripUpTo.length());
                     } else {
                         return l;
                     }
                 })
                 .forEach(content -> {
                     drain.parseLogMessage(content);
                     if (verbose && lineCounter.get() % 10000 == 0) {
                         System.out.printf("%4d clusters so far%n", drain.clusters().size());
                     }
                 });
        } catch (IOException e) {
            if(verbose) {
                e.printStackTrace(System.err);
            }
            System.exit(Main.ERR_IO_TAILING_FILE);
        }


        System.out.printf("---- Done processing file. Total of %d lines, done in %s, %d clusters%n",
                          lineCounter.get(),
                          stopwatch,
                          drain.clusters().size());
        drain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);

    }
}

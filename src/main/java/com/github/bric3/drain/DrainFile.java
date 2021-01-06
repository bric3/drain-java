package com.github.bric3.drain;

import com.github.bric3.drain.MappedFileLineReader.LineConsumer;
import com.google.common.base.Stopwatch;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DrainFile {

    private final Config config;

    public DrainFile(Config config) {
        this.config = config;
    }

    public void drain(Path file, int tailLines, boolean follow) {
        var drain = Drain.drainBuilder()
                         .additionalDelimiters("_")
                         .depth(4)
                         .build();

        var lineCounter = new AtomicInteger();
        var stopwatch = Stopwatch.createStarted();
        Consumer<String> drainConsumer = l -> {
            lineCounter.incrementAndGet();

            String content = preProcess(l);
            drain.parseLogMessage(content);
            if (config.verbose && lineCounter.get() % 10000 == 0) {
                config.out.printf("%4d clusters so far%n", drain.clusters().size());
            }
        };

        new MappedFileLineReader(config, new LineConsumer(drainConsumer, config.charset))
                .tailRead(file, tailLines, follow);


//        try {
//            Files.lines(file, StandardCharsets.UTF_8)
//                 .peek(__ -> lineCounter.incrementAndGet())
//                 .map(l -> {
//                     if (!stripUpTo.isEmpty()) {
//                         return l.substring(l.indexOf(stripUpTo) + stripUpTo.length());
//                     } else {
//                         return l;
//                     }
//                 })
//                 .forEach(content -> {
//                     drain.parseLogMessage(content);
//                     if (verbose && lineCounter.get() % 10000 == 0) {
//                         System.out.printf("%4d clusters so far%n", drain.clusters().size());
//                     }
//                 });
//        } catch (IOException e) {
//            if(verbose) {
//                e.printStackTrace(System.err);
//            }
//            System.exit(Main.ERR_IO_TAILING_FILE);
//        }

        if (config.verbose) {
            config.out.printf("---- Done processing file. Total of %d lines, done in %s, %d clusters%n",
                              lineCounter.get(),
                              stopwatch,
                              drain.clusters().size());
        }
        drain.clusters()
             .stream()
             .sorted(Comparator.comparing(LogCluster::sightings).reversed())
             .forEach(System.out::println);

    }

    private String preProcess(String line) {


        var rstripUpTo = config.drain.rstripUpTo;
        if (rstripUpTo > 0) {
            return line.substring(rstripUpTo);
        }

        var rstripUpToString = config.drain.rstripAfterString;
        if (!rstripUpToString.isEmpty()) {
            return line.substring(line.indexOf(rstripUpToString) + rstripUpToString.length());
        }
        return line;
    }
}

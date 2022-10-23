/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.tailer.drain;

import com.github.bric3.drain.core.Drain;
import com.github.bric3.drain.core.LogCluster;
import com.github.bric3.drain.internal.Stopwatch;
import com.github.bric3.tailer.config.Config;
import com.github.bric3.tailer.config.FromLine;
import com.github.bric3.tailer.file.MappedFileLineReader;
import com.github.bric3.tailer.file.MappedFileLineReader.LineConsumer;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DrainFile {

    private final Config config;

    public DrainFile(Config config) {
        this.config = config;
    }

    public void drain(Path file, FromLine fromLine, boolean follow) {
        assert file != null;
        assert fromLine != null;

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
                .tailRead(file, fromLine, follow);

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
        var parseAfterCol = config.drain.parseAfterCol;
        if (parseAfterCol > 0) {
            return line.substring(parseAfterCol);
        }

        var parseAfterStr = config.drain.parseAfterStr;
        if (!parseAfterStr.isEmpty()) {
            return line.substring(line.indexOf(parseAfterStr) + parseAfterStr.length());
        }
        return line;
    }
}

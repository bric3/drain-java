/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain;

import com.github.bric3.tailer.FromLine;
import com.github.bric3.tailer.MappedFileLineReader;
import com.github.bric3.tailer.MappedFileLineReader.ChannelSink;
import com.github.bric3.tailer.MappedFileLineReader.IOReadAction;
import com.github.bric3.tailer.MappedFileLineReader.LineConsumer;
import com.github.bric3.tailer.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MappedFileLineReaderTest {
    private final Path resourceDirectory = Paths.get("src", "test", "resources");

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Test
    void should_watch_with_line_reader(@TempDir Path tmpDir) throws IOException {
        var path = Files.createTempFile(tmpDir, "test", "log");

        var lineAppender = new LineAppender(path);
        var config = new Config(true);

        try (var r = new MappedFileLineReader(config, new LineConsumer(line -> {}, UTF_8))) {
            var future = scheduler.scheduleAtFixedRate(lineAppender, 0, 400, MILLISECONDS);
            scheduler.schedule(() -> future.cancel(false), 4, SECONDS);
            scheduler.schedule(r::close, 10, SECONDS);

            r.tailRead(path, FromLine.fromStart(0), true);

            assertThat(r.totalReadBytes()).isEqualTo(lineAppender.writtenBytes);
        }
    }

    @Test
    void should_watch_with_channel_sink(@TempDir Path tmpDir) throws IOException {
        var path = Files.createTempFile(tmpDir, "test", "log");
        var lineAppender = new LineAppender(path);
        var config = new Config(true);

        var out = new ByteArrayOutputStream();
        try (var r = new MappedFileLineReader(config, new ChannelSink(Channels.newChannel(out)))) {
            var future = scheduler.scheduleAtFixedRate(lineAppender, 0, 400, MILLISECONDS);
            scheduler.schedule(() -> future.cancel(false), 4, SECONDS);
            scheduler.schedule(r::close, 10, SECONDS);

            r.tailRead(path, FromLine.fromStart(0), true);

            assertThat(r.totalReadBytes()).isEqualTo(lineAppender.writtenBytes);
            assertThat(path.toFile()).hasBinaryContent(out.toByteArray());
        }
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdown();
    }

    @Test
    void find_start_position_given_last_lines() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"), READ)) {
            var r = new MappedFileLineReader(new Config(true), IOReadAction.NO_OP);

            assertThat(r.findTailStartPosition(channel, FromLine.fromEnd(10))).isEqualTo(0);
            assertThat(r.findTailStartPosition(channel, FromLine.fromEnd(2))).isEqualTo(42);
            assertThat(r.findTailStartPosition(channel, FromLine.fromEnd(0))).isEqualTo(183);
            assertThat(r.findTailStartPosition(channel, FromLine.fromStart(0))).isEqualTo(0);
            assertThat(r.findTailStartPosition(channel, FromLine.fromStart(2))).isEqualTo(181);
            assertThat(r.findTailStartPosition(channel, FromLine.fromStart(10))).isEqualTo(183);
        }
    }

    @Test
    void can_read_from_position() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"), READ)) {
            var sink = new ChannelSink(TestSink.nullSink());
            assertThat(sink.apply(channel, 0)).isEqualTo(183);
            assertThat(sink.apply(channel, 41)).isEqualTo(142);
        }
    }

    @Test
    void cannot_read_from_negative_position() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"), READ)) {
            var sink = new ChannelSink(TestSink.nullSink());
            assertThatExceptionOfType(AssertionError.class).isThrownBy(
                    () -> sink.apply(channel, -1)
            );
        }
    }

    static class LineAppender implements Runnable {
        Path path;
        int lineCounter = 0;
        private int writtenBytes = 0;

        public LineAppender(Path path) {
            this.path = path;
        }

        @Override
        public void run() {
            var howManyLines = new Random().nextInt(10);

            var sb = new StringBuilder();
            for (int i = 0; i <= howManyLines; i++) {
                sb.append("line ").append(lineCounter++).append("\n");
            }


            try {
                var encoded = UTF_8.encode(CharBuffer.wrap(sb));
                writtenBytes += encoded.capacity();
                Files.write(path, encoded.array(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    private static class TestSink implements WritableByteChannel {

        int writenBytes = 0;

        static TestSink nullSink() {
            return new TestSink();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {

        }

        @Override
        public int write(ByteBuffer src) {
            var remaining = src.remaining();
            writenBytes += remaining;
            return remaining;
        }
    }

}
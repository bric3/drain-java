package com.github.bric3.drain;

import com.github.bric3.drain.MappedFileLineReader.ChannelSink;
import com.github.bric3.drain.MappedFileLineReader.LineConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class MappedFileLineReaderTest {
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    @Test
    void should_watch_with_line_reader(@TempDir Path tmpDir) throws IOException {
        var path = Files.createTempFile(tmpDir, "test", "log");

        var lineAppender = new LineAppender(path);
        var config = new Config(true);

        try (var r = new MappedFileLineReader(config, new LineConsumer(line -> {}, UTF_8))) {
            var future = scheduledExecutorService.scheduleAtFixedRate(lineAppender, 0, 400, TimeUnit.MILLISECONDS);
            scheduledExecutorService.schedule(() -> future.cancel(false), 4, TimeUnit.SECONDS);
            scheduledExecutorService.schedule(r::close, 10, TimeUnit.SECONDS);

            r.watchPath(path, 0);

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
            var future = scheduledExecutorService.scheduleAtFixedRate(lineAppender, 0, 400, TimeUnit.MILLISECONDS);
            scheduledExecutorService.schedule(() -> future.cancel(false), 4, TimeUnit.SECONDS);
            scheduledExecutorService.schedule(r::close, 10, TimeUnit.SECONDS);

            r.watchPath(path, 0);

            assertThat(r.totalReadBytes()).isEqualTo(lineAppender.writtenBytes);
            assertThat(path.toFile()).hasBinaryContent(out.toByteArray());
        }
    }

    @AfterEach
    void tearDown() {
        scheduledExecutorService.shutdown();
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
}
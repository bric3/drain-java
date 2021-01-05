package com.github.bric3.drain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


class TailFileTest {

    private Path resourceDirectory = Paths.get("src", "test", "resources");

    @Test
    void find_start_position_given_last_lines() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"),
                                            StandardOpenOption.READ)) {
            var tailer = new TailFile(true);
            assertThat(tailer.findTailStartPosition(channel, 10)).isEqualTo(0);
            assertThat(tailer.findTailStartPosition(channel, 2)).isEqualTo(41);
        }
    }

    @Test
    void can_read_from_position() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"),
                                            StandardOpenOption.READ)) {
            var tailer = new TailFile(true);
            assertThat(tailer.tail(channel, 0, TestSink.nullSink())).isEqualTo(183);
            assertThat(tailer.tail(channel, 41, TestSink.nullSink())).isEqualTo(142);
        }
    }

    @Test
    void cannot_read_from_negative_position() throws IOException {
        try (var channel = FileChannel.open(resourceDirectory.resolve("3-lines.txt"),
                                            StandardOpenOption.READ)) {
            var tailer = new TailFile(true);
            assertThatExceptionOfType(AssertionError.class).isThrownBy(
                    () -> tailer.tail(channel, -1, TestSink.nullSink())
            );
        }
    }

    private static class TestSink implements WritableByteChannel {

        int writenByte = 0;

        static TestSink nullSink() {
            return new TestSink();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            var remaining = src.remaining();
            writenByte += remaining;
            return remaining;
        }
    }
}
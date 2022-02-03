/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.tailer.file;

import com.github.bric3.tailer.TailerMain;
import com.github.bric3.tailer.config.Config;
import com.github.bric3.tailer.config.FromLine;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MappedFileLineReader implements Closeable {

    private final IOReadAction readAction;
    private final Config config;

    private final AtomicBoolean closed;
    private final int wsPollTimeoutMs;
    private long totalReadBytes;

    public MappedFileLineReader(Config config, IOReadAction readAction) {
        this.readAction = readAction;
        this.config = config;
        this.wsPollTimeoutMs = 100;
        this.closed = new AtomicBoolean(false);
    }

    public void tailRead(Path path, FromLine tailFromLine, boolean follow) {
        assert path != null;
        assert tailFromLine != null;


        try (var ws = FileSystems.getDefault().newWatchService();
             var sourceChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            var startPosition = findTailStartPosition(sourceChannel, tailFromLine);
            if (config.verbose) {
                config.out.printf("Reading file from position : %d%n", startPosition);
            }

            var position = startPosition;
            var readBytes = readAction.apply(sourceChannel, startPosition);
            totalReadBytes += readBytes;
            position += readBytes;
            if (config.verbose) {
                config.out.printf("Read: %d -> %d (%d bytes)%n",
                                  startPosition,
                                  position,
                                  position - startPosition);
            }

            if (follow) {
                path.getParent().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
                while (!closed.get()) {
                    WatchKey wk;
                    try {
                        wk = ws.poll(wsPollTimeoutMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if (config.verbose) {
                            e.printStackTrace(config.err);
                        }
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (wk == null) {
                        continue;
                    }

                    for (WatchEvent<?> event : wk.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY
                            && Objects.equals(event.context(), path.getFileName())) {
                            var previousPosition = position;
                            readBytes = readAction.apply(sourceChannel, position);
                            totalReadBytes += readBytes;
                            position += readBytes;
                            if (config.verbose) {
                                config.out.printf("Read: %d -> %d (%d bytes)%n",
                                                  previousPosition,
                                                  position,
                                                  position - previousPosition);
                            }
                        }
                    }
                    var valid = wk.reset();
                    if (!valid) {
                        break; // exit
                    }
                }
            }

            totalReadBytes = position - startPosition;
            if (config.verbose) {
                config.out.printf("Total read: %d -> %d (%d bytes)%n",
                                  startPosition,
                                  position,
                                  totalReadBytes);
            }

        } catch (IOException e) {
            if (config.verbose) {
                e.printStackTrace(config.err);
            }
            System.exit(TailerMain.ERR_IO_TAILING_FILE);
        }
    }

    public long totalReadBytes() {
        return totalReadBytes;
    }

    public void close() {
        closed.set(true);
    }

    public static class LineConsumer implements IOReadAction {
        private final Consumer<String> stringConsumer;
        private final Charset charset;

        public LineConsumer(Consumer<String> stringConsumer, Charset charset) {
            this.stringConsumer = stringConsumer;
            this.charset = charset;
        }

        @Override
        public long apply(FileChannel fileChannel, long startPosition) throws IOException {
            return readByLines(fileChannel,
                               startPosition,
                               stringConsumer);
        }

        private long readByLines(FileChannel sourceChannel, long startPosition, Consumer<String> stringConsumer) throws IOException {
            var reader = Channels.newReader(sourceChannel, charset);  // investigate decoder customization
            var br = new BufferedReader(reader); // handles new lines and EOF

            sourceChannel.position(startPosition); // avoid reading the file if unnecessary
            br.lines()
              .onClose(() -> {
                  try {
                      br.close();
                  } catch (IOException ex) {
                      throw new UncheckedIOException(ex);
                  }
              })
              .forEach(stringConsumer);

            return sourceChannel.position() - startPosition;
        }
    }

    public static class ChannelSink implements IOReadAction {
        private final WritableByteChannel sink;

        public ChannelSink(WritableByteChannel sink) {
            this.sink = sink;
        }


        @Override
        public long apply(FileChannel fileChannel, long startPosition) throws IOException {
            return tail(fileChannel, startPosition, sink);
        }

        private long tail(FileChannel pathChannel,
                          long startPosition,
                          WritableByteChannel sink) throws IOException {
            assert pathChannel != null && sink != null;
            assert startPosition >= 0;
            var fileSize = pathChannel.size();

            return pathChannel.transferTo(startPosition, fileSize, sink);
        }
    }

    long findTailStartPosition(FileChannel channel, FromLine fromLine) throws IOException {
        // straw man find start position
        // this implementation hasn't been tested with two char line endings  (CR (0x0D) and LF (0x0A))
        var buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

        if (!fromLine.fromStart) {
            if (fromLine.number == 0) {
                return channel.size();
            }

            // go to end
            buffer.position((int) channel.size());

            long lineCounter = 0;
            for (long i = channel.size() - 1; i >= 0; i--) {
                char c = (char) buffer.get((int) i);

                if (c == '\n') { // on newline
                    if (lineCounter == fromLine.number) {
                        return i + 1;
                    }
                    lineCounter++;
                }
            }
            return 0;
        } else {
            if (fromLine.number == 0) {
                return 0;
            }

            long lineCounter = 0;
            for (long i = 0, channelSize = channel.size(); i < channelSize; i++) {
                char c = (char) buffer.get((int) i);

                if (c == '\n') { // on newline
                    if (lineCounter == fromLine.number) {
                        return i - 1;
                    }
                    lineCounter++;
                }
            }
            return channel.size();
        }
    }


    interface IOReadAction {
        IOReadAction NO_OP = (c, s) -> 0;

        long apply(FileChannel fileChannel, long startPosition) throws IOException;
    }
}

package com.github.bric3.drain;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
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
                while (!closed.get()) { // TODO cancel
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
            System.exit(Main.ERR_IO_TAILING_FILE);
        }
    }

    public long totalReadBytes() {
        return totalReadBytes;
    }

    public void close() {
        closed.set(true);
    }

    static class LineConsumer implements IOReadAction {
        private final Consumer<String> stringConsumer;
        private final Charset charset;

        LineConsumer(Consumer<String> stringConsumer, Charset charset) {
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
//            // option 1
//            ByteBuffer buffer = ByteBuffer.allocate(1024);
//            while (sourceChannel.read(buffer) != -1) {
//                buffer.flip();
//                System.out.print(decoder.decode(buffer)); // may fail on decoding
//                buffer.clear();
//            }

//            // option 2
//            MappedByteBuffer bb = sourceChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) sourceChannel.size()); // possible oom on big files
//            CharBuffer cb = decoder.decode(bb);

            // option 3
            var reader = Channels.newReader(sourceChannel, charset);  // investigate decoder customization
            var br = new BufferedReader(reader); // handles new lines and EOF

            // option 3.a
//        br.skip(startPosition); // read the whole file (in private method fill())
            sourceChannel.position(startPosition); // avoid reading the file if unecessary
            br.lines()
              .onClose(() -> {
                  try {
                      br.close();
                  } catch (IOException ex) {
                      throw new UncheckedIOException(ex);
                  }
              })
              .forEach(stringConsumer);

//            // option 3.b
//            br.skip(startPosition);
//            String line = br.readLine();
//            while (!line.isBlank()) {
//                stringConsumer.accept(line + "\\r\\n");
//                line = br.readLine();
//            }
            return sourceChannel.position() - startPosition;
        }
    }

    static class ChannelSink implements IOReadAction {
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
                        return i; // TODO may want to return i + 1 (to start at the line beginning)
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
            for (long i = 0, channelSize = channel.size(); i <= channelSize; i++) {
                char c = (char) buffer.get((int) i);

                if (c == '\n') { // on newline
                    if (lineCounter == fromLine.number) {
                        return i; // TODO may want to return i - 1 (to start at the line before this line ending)
                    }
                    lineCounter++;
                }
            }
            return 0;
        }
    }


    interface IOReadAction {
        IOReadAction NO_OP = (c, s) -> 0;

        long apply(FileChannel fileChannel, long startPosition) throws IOException;
    }
}

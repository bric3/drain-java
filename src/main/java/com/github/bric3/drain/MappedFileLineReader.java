package com.github.bric3.drain;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MappedFileLineReader implements Closeable {

    private static Charset charset = StandardCharsets.UTF_8;
    private static CharsetDecoder decoder = charset.newDecoder();
    private IOReadAction readAction;
    private Config config;

    private AtomicBoolean closed = new AtomicBoolean(false);
    private int wsPollTimeoutMs = 100;

    public MappedFileLineReader(Config config, IOReadAction readAction) {
        this.readAction = readAction;
        this.config = config;
    }


    public static void main(String[] args) {
        var path = Path.of("/Users/bric3/Library/Logs/JetBrains/IntelliJIdea2020.3/idea.log");

        Consumer<String> lineConsumer = line -> System.out.printf("==> %s%n", line);

        Config config = new Config(true);
        var scheduledExecutorService = Executors.newScheduledThreadPool(1);
        System.out.printf("* %s%n", "with line reader");
        try (var mappedFileLineReader = new MappedFileLineReader(config, new LineConsumer(lineConsumer))) {
            scheduledExecutorService.schedule(mappedFileLineReader::close, 5, TimeUnit.SECONDS);
            mappedFileLineReader.watchPath(path, 10);
        }
        System.out.printf("* %s%n", "with channel sink");
        try (var mappedFileChannelSink = new MappedFileLineReader(config, new ChannelSink(Channels.newChannel(System.out)))) {
            scheduledExecutorService.schedule(mappedFileChannelSink::close, 5, TimeUnit.SECONDS);
            mappedFileChannelSink.watchPath(path, 10);
        }
        System.out.printf("it's over");
    }

    public void close() {
        if (config.verbose) {
            System.out.println("Closing watcher");
        }
        closed.set(true);
    }

    private void watchPath(Path path, int tailLines) {
        assert path != null;
        assert tailLines >= 0;

        
        try (var ws = FileSystems.getDefault().newWatchService();
             var sourceChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            var startPosition = tailLines > 0 ? findTailStartPosition(sourceChannel, tailLines) : 0;
            if (config.verbose) {
                System.out.printf("Reading file from position : %d%n", startPosition);
            }

            var position = startPosition;
            position += readAction.apply(sourceChannel, startPosition);

            path.getParent().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!closed.get()) { // TODO cancel
                WatchKey wk;
                try {
                    wk = ws.poll(wsPollTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (config.verbose) {
                        e.printStackTrace(System.err);
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
                        position += readAction.apply(sourceChannel, position);
                    }
                }
                var valid = wk.reset();
                if (!valid) {
                    break; // exit
                }
            }


            if (config.verbose) {
                System.out.printf("Read file up to position : %d%n", position);
            }

        } catch (IOException e) {
            if (config.verbose) {
                e.printStackTrace(System.err);
            }
            System.exit(Main.ERR_IO_TAILING_FILE);
        }
    }

    static class LineConsumer implements IOReadAction {
        private final Consumer<String> stringConsumer;

        LineConsumer(Consumer<String> stringConsumer) {
            this.stringConsumer = stringConsumer;
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

        long tail(FileChannel pathChannel,
                  long startPosition,
                  WritableByteChannel sink) throws IOException {
            assert pathChannel != null && sink != null;
            assert startPosition >= 0;
            var fileSize = pathChannel.size();

            return pathChannel.transferTo(startPosition, fileSize, sink);
        }
    }

    long findTailStartPosition(FileChannel channel, int tailLines) throws IOException {
        // straw man find start position
        // this implementation hasn't been tested with two char line endings  (CR (0x0D) and LF (0x0A))
        var buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

        // go to end
        buffer.position((int) channel.size());

        int lineCounter = 0;
        long i;
        for (i = channel.size() - 1; i >= 0; i--) {
            char c = (char) buffer.get((int) i);

            if (c == '\n') { // on newline, reverse buffer
                if (lineCounter == tailLines) {
                    break;
                }
                lineCounter++;
            }
        }
        return Math.max(i, 0);
    }


    interface IOReadAction {
        long apply(FileChannel fileChannel, long startPosition) throws IOException;
    }
}

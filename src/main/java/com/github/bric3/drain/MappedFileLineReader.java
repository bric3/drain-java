package com.github.bric3.drain;

import java.io.BufferedReader;
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
import java.util.function.Consumer;

public class MappedFileLineReader {

    private static Charset charset = StandardCharsets.UTF_8;
    private static CharsetDecoder decoder = charset.newDecoder();
    private IOReadAction readAction;
    private Config config;


    public MappedFileLineReader(Config config, IOReadAction readAction) {
        this.readAction = readAction;
        this.config = config;
    }


    public static void main(String[] args) throws IOException {
        var path = Path.of("/Users/bric3/Library/Logs/JetBrains/IntelliJIdea2020.3/idea.log");

        Consumer<String> lineConsumer = line -> System.out.printf("==> %s%n", line);

        Config config = new Config(true);
        System.out.printf("%s%n", "with line reader");
        new MappedFileLineReader(config, new LineConsumer(lineConsumer)).watchPath(path, 10);
        System.out.printf("%s%n", "with channel sink");
        new MappedFileLineReader(config, new ChannelSink(Channels.newChannel(System.out))).watchPath(path, 10);
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

            var newPosition = readAction.apply(sourceChannel, startPosition);

            path.getParent().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) { // TODO cancel
                var wk = ws.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    var changed = (Path) event.context();
                    if (Objects.equals(changed, path.getFileName())) {
                        newPosition = readAction.apply(sourceChannel, startPosition);
                    }
                }
                var valid = wk.reset();
                if (!valid) {
                    break; // exit
                }
            }


            if (config.verbose) {
                System.out.printf("Read file up to position : %d%n", newPosition);
            }

        } catch (IOException e) {
            if (config.verbose) {
                e.printStackTrace(System.err);
            }
            System.exit(Main.ERR_IO_TAILING_FILE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (config.verbose) {
                e.printStackTrace(System.err);
            }
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
            return sourceChannel.position();
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

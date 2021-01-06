package com.github.bric3.drain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public class MappedFileLineReader {

    private static Charset charset = StandardCharsets.UTF_8;
    private static CharsetDecoder decoder = charset.newDecoder();
    private boolean verbose;

    public MappedFileLineReader(boolean verbose) {
        this.verbose = verbose;
    }


    public static void main(String[] args) throws IOException {
        var path = Path.of("/Users/bric3/Library/Logs/JetBrains/IntelliJIdea2020.3/idea.log");

        Consumer<String> stringConsumer = line -> System.out.printf("==> %s%n", line);

        new MappedFileLineReader(true).channelWatcher(path, 10, stringConsumer);
    }

    private void channelWatcher(Path path, int tailLines, Consumer<String> stringConsumer) throws IOException {
        assert path != null;
        assert tailLines >= 0;

        try (var sourceChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            var startPosition = tailLines > 0 ? findTailStartPosition(sourceChannel, tailLines) : 0;
            if(verbose) {
                System.out.printf("Reading file from position : %d%n", startPosition);
            }

            readByLines(sourceChannel, startPosition, stringConsumer);
        }
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
        br.skip(startPosition); // read the whole file (in private method fill())
        br.lines()
          .onClose(() -> {
              try {
                  br.close();
              } catch (IOException ex) {
                  throw new UncheckedIOException(ex);
              }
          })
          .skip(1)
          .forEach(stringConsumer);



//            // option 3.b
//            br.skip(startPosition);
//            String line = br.readLine();
//            while (!line.isBlank()) {
//                stringConsumer.accept(line + "\\r\\n");
//                line = br.readLine();
//            }
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

}

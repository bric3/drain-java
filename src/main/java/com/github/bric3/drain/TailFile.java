
package com.github.bric3.drain;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Objects;

public class TailFile {

    private static final WritableByteChannel STDOUT = Channels.newChannel(System.out);
    // System.out = PrintStream(BufferedOutputStream(FileOutputStream))
    // Or if POSIX : Channels.newChannel(new FileOutputStream("/dev/stdout")) to enable the system to perform zero copy?
    private final boolean verbose;

    public TailFile(boolean verbose) {
        this.verbose = verbose;
    }

    public void tail(Path path, int tailLines) {
        assert path != null;
        assert tailLines >= 0;
        try (var ws = FileSystems.getDefault().newWatchService();
             var pathChannel = FileChannel.open(path, StandardOpenOption.READ)) {

            var startPosition = tailLines > 0 ? findTailStartPosition(pathChannel, tailLines) : 0;
            if(verbose) {
                System.out.printf("Reading file from position : %d%n", startPosition);
            }
            
            var sink = STDOUT;
            var tailed = tail(pathChannel, startPosition, sink);// first tail

            path.getParent().register(ws, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) { // TODO cancel
                var wk = ws.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    var changed = (Path) event.context();
                    if (Objects.equals(changed, path.getFileName())) {
                        tailed += tail(pathChannel, tailed, sink);
                    }
                }
                var valid = wk.reset();
                if (!valid) {
                    break; // exit
                }
            }

        } catch (IOException e) {
            if (verbose) {
                e.printStackTrace(System.err);
            }
            System.exit(Main.ERR_IO_WATCHING_FILE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (verbose) {
                e.printStackTrace();
            }
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

    long tail(FileChannel pathChannel, long startPosition, WritableByteChannel sink) {
        assert pathChannel != null && sink != null;
        assert startPosition >= 0;
        try {
            var fileSize = pathChannel.size();

            return pathChannel.transferTo(startPosition, fileSize, sink);
        } catch (IOException e) {
            if (verbose) {
                e.printStackTrace(System.err);
            }
            System.exit(Main.ERR_IO_TAILING_FILE);
            return 0;
        }
    }
}

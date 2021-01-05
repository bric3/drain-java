
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

    public void tail(Path path) {
        try (var ws = FileSystems.getDefault().newWatchService();
             var pathChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            var sink = STDOUT;
            var tailed = tail(pathChannel, 0, sink);// first tail

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
            if(verbose) {
                e.printStackTrace();
            }
        }
    }

    private long tail(FileChannel pathChannel, long startPosition, WritableByteChannel sink) {
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

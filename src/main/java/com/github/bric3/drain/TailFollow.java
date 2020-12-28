
package com.github.bric3.drain;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;

public class TailFollow {

    public static final int ERR_NO_FILEPATH = 1;
    private static final int ERR_IO_READING_FILE = 2;
    private static final int ERR_IO_WATCHING_FILE = 3;
    private static final WritableByteChannel STDOUT = Channels.newChannel(System.out);
    // Or if POSIX : Channels.newChannel(new FileOutputStream("/dev/stdout")) ?


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expects a file path to tail!");
            System.exit(ERR_NO_FILEPATH);
        }

        final Path path = Paths.get(args[0]);
        if (!Files.isRegularFile(path)) {
            System.err.println("Expects a file path to tail!");
            System.exit(ERR_NO_FILEPATH);
        }

        System.err.println(path);


        long tailed = tail(path, 0);// first tail

        try (var ws = FileSystems.getDefault().newWatchService()) {
            WatchKey watchKey = path.getParent()
                                    .register(ws, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) { // TODO cancel
                var wk = ws.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    Path changed = (Path) event.context();
                    System.err.printf("%s%n", event.kind());

                    if (changed.endsWith(path.getFileName().toString())) {
                        tailed = tail(path, tailed);
                    }
                }
                wk.reset();
            }

        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(ERR_IO_WATCHING_FILE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

    }

    private static long tail(Path path, long startPosition) {
        try (var pathChannel = FileChannel.open(path, StandardOpenOption.READ)) {
            var fileSize = pathChannel.size();

            return pathChannel.transferTo(startPosition, fileSize, STDOUT);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(ERR_IO_READING_FILE);
        }
        return 0;
    }
}

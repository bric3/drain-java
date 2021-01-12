package com.github.bric3.drain;

import net.rubygrapefruit.platform.file.FileEvents;
import net.rubygrapefruit.platform.file.FileWatchEvent;
import net.rubygrapefruit.platform.file.FileWatcher;
import net.rubygrapefruit.platform.internal.Platform;
import net.rubygrapefruit.platform.internal.jni.*;
import net.rubygrapefruit.platform.test.Main;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GradleFileWatcher {

    public static void main(String[] args) throws Exception {
//        watch(Path.of("/Users/bric3/Library/Logs/JetBrains/IntelliJIdea2020.3/idea.log"));


//        LogManager.getLogManager().getLogger(NativeLogger.class.getCanonicalName()).setLevel(Level.ALL);

        Main.main(new String[] {"--machine"});
        Main.main(args);
    }

    public static void watch(Path... paths) throws InterruptedException {
        var fileNames = Arrays.stream(paths)
                              .map(Path::getFileName)
                              .map(Path::toString)
                              .collect(Collectors.toSet());

        final BlockingQueue<FileWatchEvent> eventQueue = new ArrayBlockingQueue<>(16);
        Thread processorThread = new Thread(() -> {
            final AtomicBoolean terminated = new AtomicBoolean(false);
            while (!terminated.get()) {
                FileWatchEvent event;
                try {
                    event = eventQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                event.handleEvent(new FileWatchEvent.Handler() {
                    @Override
                    public void handleChangeEvent(FileWatchEvent.ChangeType type, String absolutePath) {
                        System.out.printf("Change detected: %s / '%s'%n", type, absolutePath);
                        if (fileNames.stream().anyMatch(absolutePath::endsWith)) {
                        }
                    }

                    @Override
                    public void handleUnknownEvent(String absolutePath) {
                        System.out.printf("Unknown event happened at %s%n", absolutePath);
                    }

                    @Override
                    public void handleOverflow(FileWatchEvent.OverflowType type, String absolutePath) {
                        System.out.printf("Overflow happened (path = %s, type = %s)%n", absolutePath, type);
                    }

                    @Override
                    public void handleFailure(Throwable failure) {
                        failure.printStackTrace();
                    }

                    @Override
                    public void handleTerminated() {
                        System.out.printf("Terminated%n");
                        terminated.set(true);
                    }
                });
            }
        }, "File watcher event handler");
        processorThread.start();
        FileWatcher watcher = createWatcher(eventQueue, paths);
        try {
            System.out.println("Waiting - type 'q' to exit ...");
            while (true) {
                int ch = System.in.read();
                if (ch == 'q') {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            watcher.shutdown();
            if (!watcher.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Shutting down watcher timed out");
            }
        }
    }


    private static FileWatcher createWatcher(BlockingQueue<FileWatchEvent> eventQueue, Path... paths) throws InterruptedException {
        FileWatcher watcher;
        Logger.getLogger(NativeLogger.class.getName()).setLevel(Level.ALL);

        if (Platform.current().isMacOs()) {
            FileEvents.get(OsxFileEventFunctions.class).invalidateLogLevelCache();
            watcher = FileEvents.get(OsxFileEventFunctions.class)
                                .newWatcher(eventQueue)
//                                .withLatency(100, TimeUnit.MILLISECONDS)
                                .start();
        } else if (Platform.current().isLinux()) {
            watcher = FileEvents.get(LinuxFileEventFunctions.class)
                                .newWatcher(eventQueue)
                                .start();
        } else {
            if (!Platform.current().isWindows()) {
                throw new RuntimeException("Only Windows and macOS are supported for file watching");
            }

            watcher = FileEvents.get(WindowsFileEventFunctions.class)
                                .newWatcher(eventQueue)
                                .start();
        }

        // it seems on osx we cannot watch files directly

        var parentFolder = Arrays.stream(paths)
                            .map(Path::getParent)
                            .map(Path::toFile)
                            .collect(Collectors.toUnmodifiableList());
        System.out.printf("parent folders to watch : %s%n", parentFolder);


        watcher.startWatching(parentFolder);
        return watcher;
    }

}

package com.github.bric3.drain;

import net.rubygrapefruit.platform.file.FileEvents;
import net.rubygrapefruit.platform.file.FileWatchEvent;
import net.rubygrapefruit.platform.file.FileWatcher;
import net.rubygrapefruit.platform.internal.Platform;
import net.rubygrapefruit.platform.internal.jni.LinuxFileEventFunctions;
import net.rubygrapefruit.platform.internal.jni.OsxFileEventFunctions;
import net.rubygrapefruit.platform.internal.jni.WindowsFileEventFunctions;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GradleFileWatcher {



    public static void watch(Path path) throws InterruptedException {
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
        FileWatcher watcher = createWatcher(path, eventQueue);
        try {
            System.out.println("Waiting - type ctrl-d to exit ...");
            while (true) {
                int ch = System.in.read();
                if (ch < 0) {
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


    private static FileWatcher createWatcher(Path path, BlockingQueue<FileWatchEvent> eventQueue) throws InterruptedException {
        FileWatcher watcher;
        if (Platform.current().isMacOs()) {
            watcher = FileEvents.get(OsxFileEventFunctions.class)
                                .newWatcher(eventQueue)
                                .start();
        } else if (Platform.current().isLinux()) {
            watcher = FileEvents.get(LinuxFileEventFunctions.class)
                                .newWatcher(eventQueue)
                                .start();
        } else if (Platform.current().isWindows()) {
            watcher = FileEvents.get(WindowsFileEventFunctions.class)
                                .newWatcher(eventQueue)
                                .start();
        } else {
            throw new RuntimeException("Only Windows and macOS are supported for file watching");
        }
        watcher.startWatching(List.of(path.toFile()));
        return watcher;
    }

}

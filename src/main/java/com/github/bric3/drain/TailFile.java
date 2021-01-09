
package com.github.bric3.drain;

import com.github.bric3.drain.MappedFileLineReader.ChannelSink;

import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

public class TailFile {

    private static final WritableByteChannel STDOUT = Channels.newChannel(System.out);
    // System.out = PrintStream(BufferedOutputStream(FileOutputStream))
    // Or if POSIX : Channels.newChannel(new FileOutputStream("/dev/stdout")) to enable the system to perform zero copy?
    private final Config config;

    public TailFile(Config config) {
        this.config = config;
    }

    public void tail(Path path, FromLine fromLine, boolean follow) {
        assert path != null;
        assert fromLine != null;

        new MappedFileLineReader(config, new ChannelSink(STDOUT))
                .tailRead(path, fromLine, follow);
    }
}

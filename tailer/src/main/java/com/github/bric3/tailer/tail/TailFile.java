/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.github.bric3.tailer.tail;

import com.github.bric3.tailer.config.Config;
import com.github.bric3.tailer.config.FromLine;
import com.github.bric3.tailer.file.MappedFileLineReader;
import com.github.bric3.tailer.file.MappedFileLineReader.ChannelSink;

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

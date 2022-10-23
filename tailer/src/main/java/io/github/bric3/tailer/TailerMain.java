/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.tailer;

import io.github.bric3.tailer.config.Config;
import io.github.bric3.tailer.config.FromLine;
import io.github.bric3.tailer.config.FromLine.StartFromLineConverter;
import io.github.bric3.tailer.drain.DrainFile;
import io.github.bric3.tailer.tail.TailFile;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;


@Command(
        name = "tail",
        header = {"", "@|red tail - drain|@"},
        description = "...",
        mixinStandardHelpOptions = true,
        version = {
                "Versioned Command 1.0",
                "Picocli " + picocli.CommandLine.VERSION,
                "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
                "OS: ${os.name} ${os.version} ${os.arch}"}
)
public class TailerMain implements Runnable {
    public static final int ERR_NO_FILEPATH = 1;
    public static final int ERR_IO_TAILING_FILE = 2;
    public static final int ERR_IO_WATCHING_FILE = 3;

    public static void main(String[] args) {
        System.exit(new CommandLine(new TailerMain()).execute(args));
    }

    @Parameters(description = "log file",
                paramLabel = "FILE")
    Path file;

    @Option(names = {"-d", "--drain"},
            description = "use DRAIN to extract log patterns")
    boolean drain;

    @Option(names = {"--parse-after-str"},
            description = "when using DRAIN remove the left part of a log line up" +
                          " to after the FIXED_STRING_SEPARATOR",
            paramLabel = "FIXED_STRING_SEPARATOR")
    String parseAfterStr = "";

    @Option(names = {"--parser-after-col"},
            description = "when using DRAIN remove the left part of a log line up to COLUMN",
            paramLabel = "COLUMN")
    int parseAfterCol = 0;

    @Option(names = {"-f", "--follow"},
            description = "output appended data as the file grows")
    boolean follow;

    @Option(names = {"-n", "--lines"},
            description = "output the last NUM lines, instead of the last 10;" +
                          " or use -n 0 to output starting from beginning",
            converter = StartFromLineConverter.class,
            paramLabel = "[+]NUM",
            defaultValue = "10")
    FromLine fromLine;

    @Option(names = {"--verbose"},
            description = "Verbose output, mostly for DRAIN or errors")
    boolean verbose;

    @Option(names = {"-e"},
            description = "Experimental code",
            hidden = true)
    boolean experimental;

    @Override
    public void run() {
        if (!Files.isRegularFile(file)) {
            System.err.println("Expects a file path to tail!");
            System.exit(ERR_NO_FILEPATH);
        }

        var config = new Config(verbose, parseAfterStr, parseAfterCol);

        if (drain) {
            new DrainFile(config).drain(file, fromLine, follow);
        } else {
            new TailFile(config).tail(file, fromLine, follow);
        }

    }
}

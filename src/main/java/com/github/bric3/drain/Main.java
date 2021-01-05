package com.github.bric3.drain;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;


@Command(
        name = "tail",
        header = {"", "@|red tail - drain|@"},
        description = "...",
        mixinStandardHelpOptions = true
)
public class Main implements Runnable {
    public static final int ERR_NO_FILEPATH = 1;
    public static final int ERR_IO_TAILING_FILE = 2;
    public static final int ERR_IO_WATCHING_FILE = 3;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Parameters(description = "log file")
    Path file;

    @Option(names = {"-d", "--drain"},
            description = "Use DRAIN to extract log patterns")
    boolean drain;

    @Option(names = {"--verbose"},
            description = "Verbose output, mostly for DRAIN or errors")
    boolean verbose;


    @Override
    public void run() {
        if (drain) {
            new DrainFile(verbose).drain(file);
        } else {
            new TailFile(verbose).tail(file);
        }

    }
}

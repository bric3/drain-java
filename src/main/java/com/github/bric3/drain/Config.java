package com.github.bric3.drain;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Config {
    final boolean verbose;

    final PrintStream out;
    final PrintStream err;

    final Charset charset;

    final DrainConfig drain;

    public Config(boolean verbose) {
        this(verbose, "", 0);
    }

    public Config(boolean verbose, String rstripAfter, int rstripUpTo) {
        this.verbose = verbose;
        this.drain = new DrainConfig(rstripAfter, rstripUpTo);
        this.out = System.out;
        this.err = System.err;
        this.charset = StandardCharsets.UTF_8;
    }

    static class DrainConfig {
        final String parseAfterStr;
        final int parseAfterCol;

        DrainConfig(String parseAfterStr, int parseAfterCol) {
            this.parseAfterStr = parseAfterStr;
            this.parseAfterCol = parseAfterCol;
        }
    }
}

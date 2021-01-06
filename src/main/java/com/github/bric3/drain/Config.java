package com.github.bric3.drain;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Config {
    boolean verbose;

    PrintStream out = System.out;
    PrintStream err = System.err;

    final Charset charset = StandardCharsets.UTF_8;

    public Config(boolean verbose) {
        this.verbose = verbose;
    }
}

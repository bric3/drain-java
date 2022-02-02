/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
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

    public Config(boolean verbose, String parseAfterStr, int parseAfterCol) {
        this.verbose = verbose;
        this.drain = new DrainConfig(parseAfterStr, parseAfterCol);
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

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

public class Stopwatch {
    public static Stopwatch createStarted() {
        return new Stopwatch().start();
    }

    private long startTime;

    public Stopwatch() {
        startTime = System.currentTimeMillis();
    }

    public Stopwatch start() {
        startTime = System.currentTimeMillis();
        return this;
    }

    @Override
    public String toString() {
        return System.currentTimeMillis() - startTime + " ms";
    }
}

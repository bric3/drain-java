/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.drain.internal;

/**
 * A simple stopwatch to measure time.
 */
public class Stopwatch {
    /**
     * @return a new started stopwatch
     */
    public static Stopwatch createStarted() {
        return new Stopwatch().start();
    }

    private long startTime;

    /**
     * Create a new stopwatch.
     */
    public Stopwatch() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Starts the stopwatch (records the current time as start time).
     *
     * @return this stopwatch
     */
    public Stopwatch start() {
        startTime = System.currentTimeMillis();
        return this;
    }

    /**
     * @return the elapsed time with the millisecond unit
     */
    @Override
    public String toString() {
        return elapsed() + " ms";
    }

    /**
     * @return the elapsed time in milliseconds
     */
    private long elapsed() {
        return System.currentTimeMillis() - startTime;
    }
}

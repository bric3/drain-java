/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/**
 * Drain log pattern miner.
 * <p>
 * This code comes from a modified work of the LogPai team by IBM engineers,
 * but it has been improved to fit the Java platform.
 *
 * <p>
 * Use the builder method {@link io.github.bric3.drain.core.Drain#drainBuilder()} to configure an
 * instance.
 *
 * <p>
 * Example use:
 * <pre><code>
 * var drain = Drain.drainBuilder()
 *                  .additionalDelimiters("_")
 *                  .depth(4)
 *                  .build();
 * Files.lines(
 *     Paths.get("file.log"),
 *     StandardCharsets.UTF_8
 * ).forEach(drain::parseLogMessage);
 *
 * // do something with clusters
 * drain.clusters();
 * </code></pre>
 */
package io.github.bric3.drain.core;
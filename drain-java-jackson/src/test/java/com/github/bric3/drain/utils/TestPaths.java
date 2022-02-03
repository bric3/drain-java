/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPaths {
    public static Path get(String first, String... more) {
        Path subPath = Paths.get(first, more);
        Path resolved = Paths.get("build/resources/test").resolve(subPath);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Paths.get("..").resolve(resolved);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Paths.get("build").resolve(subPath);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Paths.get("..").resolve(resolved);
        if (Files.exists(resolved)) {
            return resolved;
        }


        throw new IllegalStateException("Could not find " + subPath);
    }
}

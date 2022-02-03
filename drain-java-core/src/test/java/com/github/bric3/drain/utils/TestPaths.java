package com.github.bric3.drain.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestPaths {
    public static Path get(String first, String... more) {
        var subPath = Path.of(first, more);
        var resolved = Path.of("build/resources/test").resolve(subPath);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Path.of("..").resolve(resolved);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Path.of("build").resolve(subPath);
        if (Files.exists(resolved)) {
            return resolved;
        }
        resolved = Path.of("..").resolve(resolved);
        if (Files.exists(resolved)) {
            return resolved;
        }


        throw new IllegalStateException("Could not find " + subPath);
    }
}

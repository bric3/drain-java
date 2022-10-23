package io.github.bric3.drain.utils;

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

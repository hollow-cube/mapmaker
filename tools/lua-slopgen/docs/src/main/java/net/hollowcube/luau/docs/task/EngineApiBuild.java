package net.hollowcube.luau.docs.task;

import net.hollowcube.luau.docs.compat.CompatReport;
import net.hollowcube.luau.docs.compat.EngineApiDiff;
import net.hollowcube.luau.docs.resolve.EngineApiAggregator;
import net.hollowcube.luau.gen.docs.EngineApi;
import net.hollowcube.luau.gen.docs.RawLibrary;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/// Static utilities tying together the aggregator, diff, and JSON I/O. Used both by the
/// `aggregateLuauApi` Gradle task and by end-to-end tests; the Gradle task itself is a thin
/// CLI wrapper ([AggregateMain]) that delegates here.
public final class EngineApiBuild {

    private static final String JSON_PREFIX = "META-INF/luau-slopgen/";

    private EngineApiBuild() {
    }

    /// Aggregate from a list of jars, scanning each for `META-INF/luau-slopgen/*.json` entries.
    public static EngineApi aggregateFromJars(List<Path> jarPaths) {
        var libs = new ArrayList<RawLibrary>();
        for (var jarPath : jarPaths) {
            try (var jar = new JarFile(jarPath.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    var name = entry.getName();
                    if (entry.isDirectory()) continue;
                    if (!name.startsWith(JSON_PREFIX)) continue;
                    if (!name.endsWith(".json")) continue;
                    try (InputStream in = jar.getInputStream(entry);
                         Reader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        libs.add(EngineApiAggregator.readRawLibrary(r));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to scan jar: " + jarPath, e);
            }
        }
        return EngineApiAggregator.aggregate(libs);
    }

    /// Aggregate from a list of bare JSON file paths (used in tests).
    public static EngineApi aggregateFromPaths(List<Path> jsonPaths) {
        var libs = new ArrayList<RawLibrary>();
        for (var p : jsonPaths) {
            try (Reader r = Files.newBufferedReader(p)) {
                libs.add(EngineApiAggregator.readRawLibrary(r));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read raw JSON: " + p, e);
            }
        }
        return EngineApiAggregator.aggregate(libs);
    }

    public static void writeEngineApi(EngineApi api, Path output) {
        try {
            Files.createDirectories(output.getParent());
            try (var w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                EngineApiAggregator.writeEngineApi(api, w);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write engine API: " + output, e);
        }
    }

    public static EngineApi readEngineApi(Path path) {
        try (Reader r = Files.newBufferedReader(path)) {
            return EngineApiAggregator.readEngineApi(r);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read engine API: " + path, e);
        }
    }

    public static CompatReport diffAgainstLockfile(EngineApi current, Path lockfile) {
        var oldApi = readEngineApi(lockfile);
        return EngineApiDiff.diff(oldApi, current);
    }
}

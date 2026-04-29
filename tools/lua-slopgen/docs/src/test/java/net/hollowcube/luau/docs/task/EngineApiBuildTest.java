package net.hollowcube.luau.docs.task;

import net.hollowcube.luau.docs.compat.DiffCategory;
import net.hollowcube.luau.docs.resolve.ResolveException;
import net.hollowcube.luau.gen.docs.RawExport;
import net.hollowcube.luau.gen.docs.RawLibrary;
import net.hollowcube.luau.gen.docs.RawMethod;
import net.hollowcube.luau.gen.docs.RawParam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class EngineApiBuildTest {

    @Test
    void aggregateFromPathsRoundTrip(@TempDir Path tmp) throws IOException {
        var libA = libraryWithExport("@e/a", "Player");
        var libB = libraryWithStaticMethod("@e/b", "find", List.of("@e/a.Player"));

        var pathA = writeJson(tmp.resolve("a.json"), libA);
        var pathB = writeJson(tmp.resolve("b.json"), libB);

        var api = EngineApiBuild.aggregateFromPaths(List.of(pathA, pathB));
        assertEquals(2, api.libraries().size());
        assertTrue(api.libraries().containsKey("@e/a"));
        assertTrue(api.libraries().containsKey("@e/b"));
    }

    @Test
    void aggregateFromJarsScansResources(@TempDir Path tmp) throws IOException {
        var libA = libraryWithExport("@e/a", "Player");
        var jarPath = tmp.resolve("engine.jar");
        var libJson = new com.google.gson.GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
            .toJson(libA);
        try (var out = new JarOutputStream(Files.newOutputStream(jarPath))) {
            out.putNextEntry(new JarEntry("META-INF/luau-slopgen/fixtures.LibA.json"));
            out.write(libJson.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        var api = EngineApiBuild.aggregateFromJars(List.of(jarPath));
        assertEquals(1, api.libraries().size());
    }

    @Test
    void aggregateRejectsInvalid(@TempDir Path tmp) throws IOException {
        var lib = libraryWithStaticMethod("@e/x", "find", List.of("Goblin"));  // unresolved
        var path = writeJson(tmp.resolve("x.json"), lib);
        assertThrows(ResolveException.class,
            () -> EngineApiBuild.aggregateFromPaths(List.of(path)));
    }

    @Test
    void writeAndReadEngineApi(@TempDir Path tmp) {
        var lib = libraryWithExport("@e/r", "Player");
        var api = EngineApiBuild.aggregateFromPaths(List.of(writeJsonUnchecked(tmp.resolve("r.json"), lib)));
        var out = tmp.resolve("engine-api.json");
        EngineApiBuild.writeEngineApi(api, out);
        assertTrue(Files.exists(out));
        var read = EngineApiBuild.readEngineApi(out);
        assertEquals(api.libraries().keySet(), read.libraries().keySet());
    }

    @Test
    void diffAgainstLockfileDetectsBreaking(@TempDir Path tmp) {
        var oldLib = libraryWithStaticMethod("@e/d", "find", List.of("number"));
        var newLib = libraryWithStaticMethod("@e/d", "find", List.of("string"));

        var oldApi = EngineApiBuild.aggregateFromPaths(
            List.of(writeJsonUnchecked(tmp.resolve("old.json"), oldLib)));
        var newApi = EngineApiBuild.aggregateFromPaths(
            List.of(writeJsonUnchecked(tmp.resolve("new.json"), newLib)));

        var lockfile = tmp.resolve("engine-api.lock.json");
        EngineApiBuild.writeEngineApi(oldApi, lockfile);

        var report = EngineApiBuild.diffAgainstLockfile(newApi, lockfile);
        assertTrue(report.hasBreakingChanges());
        assertTrue(report.findings().stream()
            .anyMatch(f -> f.category() == DiffCategory.BREAKING_RETURN_CHANGED));
    }

    @Test
    void diffAgainstUnchangedLockfileEmpty(@TempDir Path tmp) {
        var lib = libraryWithExport("@e/u", "Player");
        var api = EngineApiBuild.aggregateFromPaths(
            List.of(writeJsonUnchecked(tmp.resolve("u.json"), lib)));
        var lockfile = tmp.resolve("lock.json");
        EngineApiBuild.writeEngineApi(api, lockfile);
        var report = EngineApiBuild.diffAgainstLockfile(api, lockfile);
        assertFalse(report.hasBreakingChanges());
        assertEquals(0, report.findings().size());
    }

    // ----- helpers -----

    private static Path writeJson(Path path, RawLibrary lib) throws IOException {
        var json = new com.google.gson.GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()
            .toJson(lib);
        Files.writeString(path, json);
        return path;
    }

    private static Path writeJsonUnchecked(Path path, RawLibrary lib) {
        try {
            return writeJson(path, lib);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RawLibrary libraryWithExport(String mod, String exportLuaName) {
        return new RawLibrary(
            1, "raw-library", mod, "REQUIRE",
            "fixtures." + exportLuaName + "Lib",
            "",
            List.of(),
            List.of(),
            List.of(new RawExport(exportLuaName,
                "fixtures." + exportLuaName + "Lib." + exportLuaName,
                "", null, true, List.of(), List.of(), List.of())));
    }

    private static RawLibrary libraryWithStaticMethod(String mod, String name, List<String> returns) {
        var params = new ArrayList<RawParam>();
        return new RawLibrary(
            1, "raw-library", mod, "REQUIRE", "fixtures." + mod.replace("@", "").replace("/", "_"),
            "",
            List.of(new RawMethod(name, name, "", List.of(), params, returns)),
            List.of(),
            List.of());
    }
}

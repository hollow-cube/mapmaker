package net.hollowcube.luau.slopgen;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/// Pins the current generator output for a representative input. Updated once per intentional
/// behavior change. Run with `-Dslopgen.update_goldens=true` to refresh the expected file.
class SnapshotTest {

    @Test
    void libSampleJavaGlue() throws Exception {
        var compilation = compile();

        var glue = compilation.generatedSourceFile("fixtures.LibSample$luau")
            .orElseThrow(() -> new AssertionError("expected generated fixtures.LibSample$luau"));
        String actual = glue.getCharContent(true).toString();

        assertGolden("snapshot/LibSample$luau.expected.java", actual);
    }

    @Test
    void libSampleRawJson() throws Exception {
        var compilation = compile();

        var json = findResource(compilation, "META-INF/luau-slopgen/fixtures.LibSample.json")
            .orElseThrow(() -> new AssertionError("expected raw JSON for fixtures.LibSample"));
        var actual = json.getCharContent(true).toString();

        assertGolden("snapshot/LibSample.expected.json", actual);
    }

    private static Compilation compile() {
        return Compiler.javac()
            .withProcessors(new LuaLibraryProcessor(), new LuaAtomTableProcessor())
            .compile(JavaFileObjects.forResource("snapshot/LibSample.java"));
    }

    private static java.util.Optional<JavaFileObject> findResource(Compilation compilation, String pathSuffix) {
        for (var f : compilation.generatedFiles()) {
            if (f.getName().endsWith(pathSuffix) || f.getName().endsWith("/" + pathSuffix))
                return java.util.Optional.of(f);
        }
        return java.util.Optional.empty();
    }

    private static void assertGolden(String resourceRelativePath, String actual) throws IOException {
        Path goldenPath = Path.of("src/test/resources", resourceRelativePath);
        boolean updateRequested = Boolean.getBoolean("slopgen.update_goldens");
        if (updateRequested || !Files.exists(goldenPath)) {
            Files.createDirectories(goldenPath.getParent());
            Files.writeString(goldenPath, actual);
            if (!updateRequested)
                fail("Wrote new golden at " + goldenPath.toAbsolutePath() + " — verify content and re-run");
            return;
        }
        String expected = Files.readString(goldenPath);
        assertEquals(expected, actual);
    }

    @SuppressWarnings("unused")
    private static void assertSucceeded(Compilation c) {
        assertThat(c).succeeded();
    }
}

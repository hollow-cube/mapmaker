package net.hollowcube.scripting;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/// Pins the current generator output for a representative input. Updated once per intentional
/// behavior change. Run with `-Dslopgen.update_goldens=true` to refresh the expected file.
class SnapshotTest {

    @TempDir
    Path outDir;

    @Test
    void libSampleJavaGlue() throws Exception {
        var compilation = compile();
        assertThat(compilation).succeeded();

        var glue = compilation.generatedSourceFile("fixtures.LibSample$luau")
            .orElseThrow(() -> new AssertionError("expected generated fixtures.LibSample$luau"));
        String actual = glue.getCharContent(true).toString();

        assertGolden("snapshot/LibSample$luau.expected.java", actual);
    }

    @Test
    void libSampleEngineApiJson() throws Exception {
        var compilation = compile();
        assertThat(compilation).succeeded();

        var engineApi = outDir.resolve("engine-api.json");
        if (!Files.exists(engineApi))
            throw new AssertionError("expected engine-api at " + engineApi + " but the AP did not write it");

        assertGolden("snapshot/LibSample.expected.json", Files.readString(engineApi));
    }

    private Compilation compile() {
        return Compiler.javac()
            .withProcessors(new LuaApiProcessor())
            .withOptions("-A" + LuaApiProcessor.OUTPUT_DIR_OPTION + "=" + outDir.toAbsolutePath())
            .compile(JavaFileObjects.forResource("snapshot/LibSample.java"));
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
}

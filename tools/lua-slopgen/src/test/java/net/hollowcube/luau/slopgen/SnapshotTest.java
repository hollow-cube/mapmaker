package net.hollowcube.luau.slopgen;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

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
    void libSampleSnapshot() throws Exception {
        Compilation compilation = Compiler.javac()
            .withProcessors(new LuaLibraryProcessor(), new LuaAtomTableProcessor())
            .compile(JavaFileObjects.forResource("snapshot/LibSample.java"));

        assertThat(compilation).succeeded();

        var glue = compilation.generatedSourceFile("fixtures.LibSample$luau")
            .orElseThrow(() -> new AssertionError("expected generated fixtures.LibSample$luau"));
        String actual = glue.getCharContent(true).toString();

        assertGolden("snapshot/LibSample$luau.expected.java", actual);
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

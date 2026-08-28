package net.hollowcube.sqlgen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Pins the generator's output for the sample corpus.
///
/// Generated sources are committed, so any change in what the emitters produce should show up as a
/// reviewable diff rather than as a surprise the next time someone regenerates. Run the
/// `sqlGenSample` task to accept a deliberate change.
class GoldenTest {

    private static final Path SAMPLE = Path.of("sample");
    private static final String PACKAGE = "sample.db";

    @Test
    void sampleCorpusMatchesTheCommittedOutput() throws IOException {
        var generated = SqlGen.generate(SAMPLE.resolve("db/migrations"), SAMPLE.resolve("db/queries"),
            PACKAGE, "SampleDatabase");
        var golden = SAMPLE.resolve("generated");

        assertEquals(String.join("\n", existing(golden)), String.join("\n", new TreeSet<>(generated.keySet())),
            "generated file set");
        for (var entry : generated.entrySet()) {
            assertEquals(Files.readString(golden.resolve(entry.getKey())), entry.getValue(), entry.getKey());
        }
    }

    private static TreeSet<String> existing(Path golden) throws IOException {
        if (!Files.isDirectory(golden)) return new TreeSet<>();
        try (var files = Files.walk(golden)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(path -> golden.relativize(path).toString())
                .collect(Collectors.toCollection(TreeSet::new));
        }
    }

}

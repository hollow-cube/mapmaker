package net.hollowcube.apiserver.chatfilter;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// [ChatFilter] end to end against what python's onnxruntime scored the same texts, in
/// `golden.jsonl`. Not exact: int8 kernels differ between cpus, so this allows what that moves a
/// probability by and catches a wrong input or output far beyond it.
///
/// Needs `CHAT_FILTER_DIR`, like [ChatTokenizerTest], and the onnxruntime shared library in
/// `ONNXRUNTIME_LIBRARY`.
class ChatFilterTest {
    private static final double TOLERANCE = 5e-3;

    private static ChatFilter filter;

    @BeforeAll
    static void load() throws IOException {
        var dir = System.getenv("CHAT_FILTER_DIR");
        var library = System.getenv("ONNXRUNTIME_LIBRARY");
        assumeTrue(
            dir != null && library != null,
            "CHAT_FILTER_DIR or ONNXRUNTIME_LIBRARY not set"
        );
        filter = ChatFilter.load(Path.of(library), Path.of(dir));
    }

    @AfterAll
    static void close() {
        if (filter != null) filter.close();
    }

    @Test
    void matchesPython() throws IOException {
        var stream = Objects.requireNonNull(
            getClass().getResourceAsStream("/chatfilter/golden.jsonl")
        );
        var failures = new ArrayList<String>();
        double worst = 0;
        try (
            var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        ) {
            for (String line; (line = reader.readLine()) != null;) {
                var row = JsonParser.parseString(line).getAsJsonObject();
                var expected = row.getAsJsonArray("scores")
                    .asList()
                    .stream()
                    .mapToDouble(e -> e.getAsDouble())
                    .toArray();
                var actual = filter.score(row.get("text").getAsString()).values();
                for (int i = 0; i < expected.length; i++) {
                    var diff = Math.abs(expected[i] - actual.get(i));
                    worst = Math.max(worst, diff);
                    if (diff > TOLERANCE) {
                        failures.add(
                            row.get("text")
                                + "\n  expected "
                                + Arrays.toString(expected)
                                + "\n  actual   "
                                + actual
                        );
                        break;
                    }
                }
            }
        }
        System.out.println("worst difference " + worst);
        assertTrue(
            failures.isEmpty(),
            failures.size()
                + " differ:\n"
                + String.join("\n", failures.subList(0, Math.min(20, failures.size())))
        );
    }
}

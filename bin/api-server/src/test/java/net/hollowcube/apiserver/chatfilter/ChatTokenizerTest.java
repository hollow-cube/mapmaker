package net.hollowcube.apiserver.chatfilter;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Holds [ChatTokenizer] to the ids huggingface tokenizers gives, written by the training repo's
/// `deploy/golden.py`. A tokenizer that is off by one id still runs and still scores, just worse,
/// so nothing else would catch it.
///
/// Needs a chat filter version directory in `CHAT_FILTER_DIR`; `CHAT_FILTER_GOLDEN` adds a
/// golden file of real chat, which is only ever generated locally.
class ChatTokenizerTest {
    private static ChatTokenizer tokenizer;

    @BeforeAll
    static void load() throws IOException {
        var dir = System.getenv("CHAT_FILTER_DIR");
        assumeTrue(dir != null, "CHAT_FILTER_DIR not set");
        tokenizer = ChatTokenizer.load(Path.of(dir, "tokenizer.bin"));
    }

    @Test
    void committedGolden() throws IOException {
        var stream = Objects.requireNonNull(
            getClass().getResourceAsStream("/chatfilter/golden.jsonl")
        );
        try (
            var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
        ) {
            assertGolden(reader);
        }
    }

    @Test
    void localGolden() throws IOException {
        var file = System.getenv("CHAT_FILTER_GOLDEN");
        assumeTrue(file != null, "CHAT_FILTER_GOLDEN not set");
        try (var reader = Files.newBufferedReader(Path.of(file))) {
            assertGolden(reader);
        }
    }

    private static void assertGolden(BufferedReader reader) throws IOException {
        var failures = new ArrayList<String>();
        int count = 0;
        for (String line; (line = reader.readLine()) != null; count++) {
            var row = JsonParser.parseString(line).getAsJsonObject();
            var text = row.get("text").getAsString();
            var expected = row.getAsJsonArray("ids")
                .asList()
                .stream()
                .mapToLong(e -> e.getAsLong())
                .toArray();
            var actual = tokenizer.encode(text);
            if (!Arrays.equals(expected, actual))
                failures.add(
                    "%s\n  expected %s\n  actual   %s".formatted(
                        row.get("text"),
                        Arrays.toString(expected),
                        Arrays.toString(actual)
                    )
                );
        }
        assertTrue(count > 0, "empty golden file");
        assertTrue(
            failures.isEmpty(),
            failures.size()
                + "/"
                + count
                + " differ:\n"
                + String.join("\n", failures.subList(0, Math.min(20, failures.size())))
        );
    }
}

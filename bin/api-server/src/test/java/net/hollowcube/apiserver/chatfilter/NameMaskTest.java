package net.hollowcube.apiserver.chatfilter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Expected outputs are what `chat_filter/names.py` gives for the same words and names.
class NameMaskTest {
    private static NameMask mask;

    @BeforeAll
    static void load(@TempDir Path dir) throws IOException {
        var words = dir.resolve("words.txt");
        Files.writeString(words, "castle\nplace\nrun\ngay\ngoon\nlol\n");
        mask = NameMask.load("@player", words);
        mask.setNames(List.of("WeGay", "castle", "placing", "ab", "Patua", "runners", "goon", "Skeptikql", "x_Y_z"));
    }

    @Test
    void masksNames() {
        assertEquals("@player is cheating", mask.mask("WeGay is cheating"));
        assertEquals("@player: gg", mask.mask("skeptikql: gg"));
        assertEquals("@player @player!", mask.mask("@WEGAY x_y_z!"));
    }

    @Test
    void leavesWordsAndTheirInflections() {
        assertEquals("castle placing near @player", mask.mask("castle placing near @patua"));
        assertEquals("ab runners goon", mask.mask("ab runners goon"));
    }

    @Test
    void tokensAreUnicodeWords() {
        assertEquals("héllo patuaé @player", mask.mask("héllo patuaé patua"));
    }
}

package net.hollowcube.terraform.util.script;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.suggestion.SuggestionEntry;
import net.hollowcube.terraform.TerraformRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Assertions {

    public static void assertSuggestions(@NotNull ParseTree<?> tree, @NotNull String... expected) {
        assertSuggestions(tree, List.of(expected));
    }

    public static void assertSuggestions(@NotNull ParseTree<?> tree, @NotNull List<String> expected) {
        var suggestion = new Suggestion(0, 0);
        tree.suggest(TerraformRegistry.EMPTY, suggestion);
        var actual = suggestion.getEntries().stream().map(SuggestionEntry::replacement).toList();

        assertEquals(expected, actual);
    }
}

package net.hollowcube.command.full;

import net.hollowcube.command.BaseCommandTest;
import net.hollowcube.command.CommandBuilder;
import net.hollowcube.command.arg.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.params.provider.Arguments.of;

public class MapCommandTest extends BaseCommandTest {

    @BeforeEach
    void createCommandGraph() {
        var playerArg = Argument.Word("player").with("sethprg", "notmattw");
        var sizeArg = Argument.Word("size").with("small", "medium", "large");

        manager.register("map", new CommandBuilder()
                .child("list", list -> list
                        .executes(mockExecutor())
                        .executes(mockExecutor(), playerArg))
                .child("alter", alter -> alter
                        .child("size", size -> size
                                .executes(mockExecutor(), sizeArg)
                        ))
                .child("lb", lb -> lb
                        .condition(condHide)
                        .child("restore", restore -> restore
                                .executes(mockExecutor())))
                .child("delete", delete -> delete
                        .condition(condDeny)
                        .executes(mockExecutor()))
                .node());
    }

    // Executions

    @ValueSource(strings = {
            "map list",
            "map list sethprg",
            "map alter size small",
            "map alter size medium",
            "map alter size large"
    })
    @ParameterizedTest
    void execGenericValidSyntaxes(@NotNull String syntax) {
        assertSuccess(syntax);
    }

    private static @NotNull Stream<Arguments> execGenericInvalidSyntaxesSource() {
        return Stream.of(
                of("map", 3),
                of("map list sethprg seth", 17),
                of("map alter size med", 15)
        );
    }

    @MethodSource("execGenericInvalidSyntaxesSource")
    @ParameterizedTest
    void execGenericInvalidSyntaxes(@NotNull String input, int errorStart) {
        assumeFalse(true, "TODO: fix this");
        var err = assertSyntaxError(input);
        assertEquals(errorStart, err.start());
    }

    // Suggestions

    @ValueSource(strings = {
            "map ###list,alter",
            "map list###",
            "map list ###sethprg,notmattw",
            "map alter###",
            "map alter ###size",
            "map alter size ###small,medium,large",
    })
    @ParameterizedTest
    void testSuggestions(@NotNull String testCase) {
        assumeFalse(true, "TODO: fix this");
        var parts = testCase.split("###");
        String input = parts[0], expected = "";
        if (parts.length > 1) expected = parts[1];

        assertSuggestions(input, Stream.of(expected.split(",")).toArray(String[]::new));
    }
}

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
import static org.junit.jupiter.params.provider.Arguments.of;

public class GameRuleCommandTest extends BaseCommandTest {

    @BeforeEach
    void createCommandGraph() {
        var boolArg = Argument.Word("value").with("true", "false");
        var intArg = Argument.Word("value");

        manager.register("gamerule", new CommandBuilder()
                .child("doDaylightCycle", doDaylightCycle -> doDaylightCycle
                        .executes(mockExecutor())
                        .executes(mockExecutor(), boolArg))
                .child("randomTickSpeed", randomTickSpeed -> randomTickSpeed
                        .executes(mockExecutor())
                        .executes(mockExecutor(), intArg))
                .node());
    }

    // Executions

    @ValueSource(strings = {
            "gamerule doDaylightCycle",
            "gamerule doDaylightCycle true",
            "gamerule doDaylightCycle false",
            "gamerule randomTickSpeed",
            "gamerule randomTickSpeed 1",
    })
    @ParameterizedTest
    void execGenericValidSyntaxes(@NotNull String syntax) {
        assertSuccess(syntax);
    }

    private static @NotNull Stream<Arguments> execGenericInvalidSyntaxesSource() {
        return Stream.of(
                of("gamerule", 8),
                of("gamerule doDaylightCycle 1", 25)
        );
    }

    @MethodSource("execGenericInvalidSyntaxesSource")
    @ParameterizedTest
    void execGenericInvalidSyntaxes(@NotNull String input, int errorStart) {
        var err = assertSyntaxError(input);
        assertEquals(errorStart, err.start());
    }

    // Suggestions

    @ValueSource(strings = {
            "gamerule ###doDaylightCycle,randomTickSpeed",
            "gamerule doDaylightCy###doDaylightCycle",
            "gamerule doDaylightCycle ###true,false",
    })
    @ParameterizedTest
    void testSuggestions(@NotNull String testCase) {
        var parts = testCase.split("###");
        String input = parts[0], expected = "";
        if (parts.length > 1) expected = parts[1];

        assertSuggestions(input, Stream.of(expected.split(",")).toArray(String[]::new));
    }
}

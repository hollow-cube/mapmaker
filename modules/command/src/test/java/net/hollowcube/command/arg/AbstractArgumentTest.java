package net.hollowcube.command.arg;

import net.hollowcube.command.util.FakePlayer;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractArgumentTest {

    static {
        MinecraftServer.init();
    }

    abstract Argument<?> argument();

    abstract Stream<Arguments> testCases();


    @MethodSource("testCases")
    @ParameterizedTest(name = "{0}")
    void testArgumentCase(String name, String input, Object expected) {
        var result = argument().parse(FakePlayer.CONSOLE, new StringReader(input));
        var success = assertInstanceOf(ParseResult.Success.class, result);
        assertEquals(expected, success.valueFunc().get());
    }

}

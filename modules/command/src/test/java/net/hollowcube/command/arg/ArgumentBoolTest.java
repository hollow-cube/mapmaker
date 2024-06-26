package net.hollowcube.command.arg;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class ArgumentBoolTest extends AbstractArgumentTest {

    @Override
    Argument<?> argument() {
        return new ArgumentBool("test");
    }

    @Override
    Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("true", "true", true),
                Arguments.of("false", "false", false)
        );
    }
}

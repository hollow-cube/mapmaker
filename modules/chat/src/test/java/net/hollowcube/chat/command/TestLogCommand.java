package net.hollowcube.chat.command;

import net.hollowcube.chat.ChatQuery;
import net.hollowcube.chat.TestChatFacet;
import net.hollowcube.chat.storage.MockChatStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.CommandDispatcher;
import net.minestom.server.command.builder.CommandResult;
import net.minestom.server.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestLogCommand {
//    private final CommandDispatcher dispatcher = new CommandDispatcher();
//    // Fine to use headless player since this command does not affect the world or anything
//    private final Player player = TestChatFacet.player;
//    private final MockChatStorage storage = new MockChatStorage();
//
//    @BeforeEach
//    public void setup() {
//        dispatcher.register(new LogCommand(storage));
//    }
//
//    private static Stream<Arguments> commandToQueryMappings() {
//        return Stream.of(
//                Arguments.of("log in global", ChatQuery.builder().context("global").build()),
//                Arguments.of("log context global", ChatQuery.builder().context("global").build()),
//                Arguments.of("log in global in local", ChatQuery.builder().context("global", "local").build()),
//                Arguments.of("log on build", ChatQuery.builder().serverId("build").build()),
//                Arguments.of("log on build in global", ChatQuery.builder().context("global").serverId("build").build())
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("commandToQueryMappings")
//    void basicExecution(String command, ChatQuery expectedQuery) {
//        MinecraftServer.init();
//        dispatcher.execute(player, command);
//
//        assertThat(storage.queries()).containsExactly(expectedQuery);
//    }
}

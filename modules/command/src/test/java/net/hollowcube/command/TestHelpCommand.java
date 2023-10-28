package net.hollowcube.command;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.example.FlipCommand;
import net.hollowcube.command.example.ListCommand;
import net.hollowcube.command.example.ParentCommand;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.suggestion.SuggestionEntry;
import net.hollowcube.command.util.FakePlayer;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestHelpCommand {

    private final CommandManager manager = new CommandManager();
    private final HelpCommand command = new HelpCommand(manager);
    private final StringReader emptyReader = new StringReader("");

    private final CommandSender player = new FakePlayer();
    private final CommandSender console = FakePlayer.CONSOLE;

    @BeforeEach
    void setup() {
        manager.register(command);
        manager.register(new FlipCommand(null, null));
        manager.register(new ListCommand());
        manager.register(new ParentCommand());
    }

    @Nested
    class Suggestions {

        @Test
        void emptyInputSuggestAll() {
            var suggestion = new Suggestion(0, 0);
            command.suggestCommand(player, emptyReader, suggestion, "");
            assertEquals(4, suggestion.getEntries().size());
            assertArrayEquals(
                    new String[]{"flip", "help", "list", "parent"},
                    suggestion.getEntries().stream().map(SuggestionEntry::replacement).sorted().toArray(String[]::new)
            );
        }

        @Test
        void partialInputSuggestSingle() {
            var suggestion = new Suggestion(0, 0);
            command.suggestCommand(player, emptyReader, suggestion, "fl");
            assertEquals(1, suggestion.getEntries().size());
            assertEquals("flip", suggestion.getEntries().get(0).replacement());
        }

        @Test
        void subcommandSuggestAll() {
            var suggestion = new Suggestion(0, 0);
            command.suggestCommand(player, emptyReader, suggestion, "parent ");
            assertEquals(1, suggestion.getEntries().size());
            assertEquals("list", suggestion.getEntries().get(0).replacement());
        }

        @Test
        void subcommandSuggestFiltered() {
            var suggestion = new Suggestion(0, 0);
            command.suggestCommand(player, emptyReader, suggestion, "parent l");
            assertEquals(1, suggestion.getEntries().size());
            assertEquals("list", suggestion.getEntries().get(0).replacement());
        }

        @Test
        void noMatchSuggestNothing() {
            var suggestion = new Suggestion(0, 0);
            command.suggestCommand(player, emptyReader, suggestion, "nothing");
            assertEquals(0, suggestion.getEntries().size());
        }
    }

    @Nested
    class Parse {

        @Test
        void emptyInputMatchNothing() {
            var result = command.resolveCommand(player, "");
            assertInstanceOf(Argument.ParsePartial.class, result);
        }

        @Test
        void partialInputMatchNothing() {
            var result = command.resolveCommand(player, "fl");
            assertInstanceOf(Argument.ParsePartial.class, result);
        }

        @Test
        void exactMatch() {
            var result = command.resolveCommand(player, "flip");
            var resolved = (HelpCommand.ResolvedCommand) assertInstanceOf(Argument.ParseSuccess.class, result).value();
            assertEquals(0, resolved.path().size());
            assertEquals("flip", resolved.command().name());
        }

        @Test
        void subcommandSpaceMatchNothing() {
            var result = command.resolveCommand(player, "parent ");
            assertInstanceOf(Argument.ParsePartial.class, result);
        }

        @Test
        void subcommandExactMatch() {
            var result = command.resolveCommand(player, "parent list");
            var resolved = (HelpCommand.ResolvedCommand) assertInstanceOf(Argument.ParseSuccess.class, result).value();
            assertEquals(1, resolved.path().size());
            assertEquals("parent", resolved.path().get(0));
            assertEquals("list", resolved.command().name());
        }
    }

}

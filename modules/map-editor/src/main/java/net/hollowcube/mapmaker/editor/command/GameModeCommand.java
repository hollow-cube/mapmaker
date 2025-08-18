package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

/**
 * This command is hidden and is used for the purpose of vanilla game mode switching such as f3 + f4 and f3 + n.
 */
public class GameModeCommand extends CommandDsl {

    private final Argument<String> argument = Argument.Word("gamemode");

    public GameModeCommand() {
        super("gamemode");

        this.category = CommandCategory.HIDDEN;

        setCondition(CommandCondition.and(
                (_, context) -> context.pass() == CommandContext.Pass.BUILD ? CommandCondition.HIDE : CommandCondition.ALLOW,
                builderOnly()
        ));
        addSyntax(playerOnly(this::setGamemode), argument);
    }

    private void setGamemode(Player player, CommandContext context) {
        var gamemode = switch (context.get(argument)) {
            case "creative" -> GameMode.CREATIVE;
            case "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
        if (gamemode == null) return;
        player.setGameMode(gamemode);
    }

}

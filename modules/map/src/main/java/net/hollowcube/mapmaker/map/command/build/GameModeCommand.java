package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

/**
 * This command is hidden and is used for the purpose of vanilla game mode switching such as f3 + f4 and f3 + n.
 */
public class GameModeCommand extends CommandDsl {

    private final Argument<String> argument = Argument.Word("gamemode");

    public GameModeCommand() {
        super("gamemode");

        this.category = CommandCategory.HIDDEN;

        setCondition(CommandCondition.and(
                ($1, context) -> context.pass() == CommandContext.Pass.BUILD ? CommandCondition.HIDE : CommandCondition.ALLOW,
                mapFilter(false, true, false)
        ));
        addSyntax(playerOnly(this::setGamemode), argument);
    }

    private void setGamemode(@NotNull Player player, @NotNull CommandContext context) {
        var gamemode = switch (context.get(argument)) {
            case "creative" -> GameMode.CREATIVE;
            case "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
        if (gamemode == null) return;
        player.setGameMode(gamemode);
    }

}

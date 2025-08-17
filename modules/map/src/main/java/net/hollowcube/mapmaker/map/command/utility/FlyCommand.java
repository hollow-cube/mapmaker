package net.hollowcube.mapmaker.map.command.utility;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;
import static net.hollowcube.mapmaker.map.util.MapCondition.spectatorFilter;

public class FlyCommand extends CommandDsl {
    public FlyCommand() {
        super("fly");
        setCondition(CommandCondition.or(
                mapFilter(false, true, false),
                spectatorFilter(true, false, true)
        ));

        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(@NotNull Player player, @NotNull CommandContext context) {
        // Spectator handler will handle flight for all players but if spectator will send event
        if (SpectateHandler.toggleFlight(player)) {
            player.sendMessage(MapMessages.COMMAND_FLY_ENABLED);
        } else {
            player.sendMessage(MapMessages.COMMAND_FLY_DISABLED);
        }
    }
}

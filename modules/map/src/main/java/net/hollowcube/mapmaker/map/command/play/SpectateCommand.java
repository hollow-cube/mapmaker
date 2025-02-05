package net.hollowcube.mapmaker.map.command.play;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.map;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class SpectateCommand extends CommandDsl {

    public SpectateCommand() {
        super("spectate", "spec", "practice", "prac");

        category = CommandCategories.MAP;
        description = "Spectate the current map";

        setCondition(CommandCondition.and(
                mapFilter(true, false, false, true),
                map(map -> map.map().getSetting(MapSettings.NO_SPECTATOR) != Boolean.TRUE)
        ));
        addSyntax(playerOnly(this::execute));
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        SpectateHandler.setSpectating(player, TriState.NOT_SET);
    }

}

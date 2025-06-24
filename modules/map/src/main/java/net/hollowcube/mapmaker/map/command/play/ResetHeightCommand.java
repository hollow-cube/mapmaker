package net.hollowcube.mapmaker.map.command.play;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.feature.play.ResetHeightDisplayFeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class ResetHeightCommand extends CommandDsl {

    public ResetHeightCommand() {
        super("showheight");

        category = CommandCategories.MAP;
        description = "Toggles a reset height display for the map";

        setCondition(mapFilter(true, false, true, false));
        addSyntax(playerOnly(this::execute));
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        ResetHeightDisplayFeatureProvider.toggle(player);
    }

}

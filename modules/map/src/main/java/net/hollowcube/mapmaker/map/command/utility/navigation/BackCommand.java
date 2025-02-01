package net.hollowcube.mapmaker.map.command.utility.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.feature.edit.TeleportHistoryFeatureProvider;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class BackCommand extends CommandDsl {

    private static final Component NO_PREVIOUS_LOCATION = Component.translatable("commands.back.no_previous_location");
    private static final Component SUCCESS = Component.translatable("commands.back.success");

    public BackCommand() {
        super("back");

        category = CommandCategories.MAP;
        description = "Teleports you back to your last location you teleported from.";

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::execute));
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var lastLocation = player.getTag(TeleportHistoryFeatureProvider.LAST_LOCATION);
        if (lastLocation == null) {
            player.sendMessage(NO_PREVIOUS_LOCATION);
        } else {
            MapWorldHelpers.teleportPlayer(player, lastLocation).thenRun(() -> player.sendMessage(SUCCESS));
        }
    }
}


package net.hollowcube.mapmaker.dev.command.map.world;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.map.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapWorldListCommand extends CommandDsl {
    private final MapWorldManager mwm;

    public MapWorldListCommand(@NotNull MapWorldManager mwm) {
        super("list");
        this.mwm = mwm;

        addSyntax(playerOnly(this::handleListWorlds));
    }

    private void handleListWorlds(@NotNull Player player, @NotNull CommandContext context) {
        var activeMaps = mwm.getActiveMaps();
        if (activeMaps.isEmpty()) {
            player.sendMessage("no active maps");
            return;
        }

        player.sendMessage("active maps:");
        for (var map : activeMaps.entrySet()) {
            var instanceId = map.getKey();
            var loadingMap = map.getValue();

            var builder = Component.text().append(Component.text("• "));
            if (!loadingMap.isDone()) {
                builder.append(Component.text(instanceId.id())).append(Component.text(" (loading)"));
            } else {
                var world = FutureUtil.getUnchecked(loadingMap);
                builder.append(MapData.createMapHoverText(world.map()))
                        .append(Component.text(" ("))
                        .append(Component.text(world.players().size()))
                        .append(Component.text(" players)"));
            }
            player.sendMessage(builder.build());
        }
    }

}

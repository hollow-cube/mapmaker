package net.hollowcube.mapmaker.map.command.utility.entity;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.kyori.adventure.nbt.TagStringIOExt;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class EntitiesCommand extends CommandDsl {
    public EntitiesCommand() {
        super("entities");
        setCondition(mapFilter(false, true, false));

        description = "List all entities in the map";
        category = CommandCategories.UTILITY;

        addSyntax(playerOnly(this::handleQueryAll));
    }

    private void handleQueryAll(@NotNull Player player, @NotNull CommandContext context) {
        var entities = player.getInstance().getEntities().stream()
                .filter(e -> !(e instanceof Player))
                .toList();

        if (entities.isEmpty()) {
            player.sendMessage("No entities found in the map");
            return;
        }

        player.sendMessage("Entities in the map:");
        entities.forEach(entity -> {
            player.sendMessage(" - " + entity.getEntityType() + " at " + entity.getPosition());
            if (entity instanceof MarkerEntity marker) {
                player.sendMessage("   " + TagStringIOExt.writeTag(marker.getMarkerData()));
            }
        });
    }
}

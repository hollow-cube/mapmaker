package net.hollowcube.mapmaker.map.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapType;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BoxFeatureProvider implements FeatureProvider {

    private final static int MIN_X = 0;
    private final static int MAX_X = 14;
    private final static int MIN_Y = 37;
    private final static int MAX_Y = 55;
    private final static int MIN_Z = 0;
    private final static int MAX_Z = 14;

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map:box", EventFilter.INSTANCE)
            .addListener(PlayerBlockBreakEvent.class, this::handleBlockBreak)
            .addListener(PlayerBlockPlaceEvent.class, this::handleBlockPlace);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!world.map().type().equals(MapType.BOX))
            return false;

        if (!(world instanceof EditingMapWorld))
            return false;

        world.eventNode().addChild(eventNode);

        return true;
    }

    private void handleBlockPlace(@NotNull PlayerBlockPlaceEvent event) {
        Player player = event.getPlayer();
        Point block_pos = event.getBlockPosition();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        // Deny placing outside box
        if (block_pos.x() < MIN_X || block_pos.x() > MAX_X ||
            block_pos.y() < MIN_Y || block_pos.y() > MAX_Y ||
            block_pos.z() < MIN_Z || block_pos.z() > MAX_Z) {
            player.sendMessage("Cannot place blocks outside box!");
            event.setCancelled(true);
        }
    }

    private void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        Player player = event.getPlayer();
        Point block_pos = event.getBlockPosition();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        // Deny breaking outside box
        if (block_pos.x() < MIN_X || block_pos.x() > MAX_X ||
                block_pos.y() < MIN_Y || block_pos.y() > MAX_Y ||
                block_pos.z() < MIN_Z || block_pos.z() > MAX_Z) {
            player.sendMessage("Cannot break blocks outside box!");
            event.setCancelled(true);
        }
    }
}

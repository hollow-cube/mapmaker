package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.block.handler.AbstractPlateHandler;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.feature.checkpoint.gui.CheckpointSettingsView;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class CheckpointPlateBlock extends AbstractPlateHandler {
    public static final String POI_TYPE = "mapmaker:checkpoint_plate";

    public static final NamespaceID ID = NamespaceID.from("mapmaker:checkpoint_plate");

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var mapWorld = MapWorld.fromInstance(tick.getInstance());
        var checkpoint = mapWorld.map().getPoi(tick.getBlockPosition());
        EventDispatcher.call(new MapWorldCheckpointReachedEvent(mapWorld, player, checkpoint));
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        var map = MapWorld.fromInstance(placement.getInstance()).map();
        map.addPOI(new MapData.POI(POI_TYPE, UUID.randomUUID().toString(), placement.getBlockPosition()));
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var world = MapWorld.fromInstance(interaction.getInstance());
        if (!(world instanceof EditingMapWorld)) return false;

        var player = interaction.getPlayer();
        if (player.isSneaking()) return false;

        // Open checkpoint settings GUI
        var checkpoint = world.map().getPoi(interaction.getBlockPosition());
        world.server().newOpenGUI(player, c -> new CheckpointSettingsView(c.with(Map.of("poi", checkpoint))));

        return true;
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        var map = MapWorld.fromInstance(destroy.getInstance()).map();
        map.removePOI(destroy.getBlockPosition());
    }
}

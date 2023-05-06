package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.block.handler.AbstractPlateHandler;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.feature.checkpoint.gui.CheckpointSettingsView;
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
        var mapWorld = MapWorld.forPlayer(player);
        var checkpoint = mapWorld.map().getPoi(tick.getBlockPosition());
        EventDispatcher.call(new MapWorldCheckpointReachedEvent(mapWorld, player, checkpoint));
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        MapData map;
        if (placement instanceof PlayerPlacement pp) {
            map = MapWorld.forPlayer(pp.getPlayer()).map();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorld.unsafeFromInstance(placement.getInstance());
            if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) return;
            map = world.map();
        }
        map.addPOI(new MapData.POI(POI_TYPE, UUID.randomUUID().toString(), placement.getBlockPosition()));
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var world = MapWorld.forPlayer(interaction.getPlayer());
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0) return false;

        var player = interaction.getPlayer();
        if (player.isSneaking()) return false;

        // Open checkpoint settings GUI
        var checkpoint = world.map().getPoi(interaction.getBlockPosition());
        world.server().newOpenGUI(player, c -> new CheckpointSettingsView(c.with(Map.of("poi", checkpoint))));

        return true;
    }

    @Override
    public void onDestroy(@NotNull Destroy destroy) {
        MapData map;
        if (destroy instanceof PlayerDestroy pd) {
            map = MapWorld.forPlayer(pd.getPlayer()).map();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorld.unsafeFromInstance(destroy.getInstance());
            if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) return;
            map = world.map();
        }
        map.removePOI(destroy.getBlockPosition());
    }
}

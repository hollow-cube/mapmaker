package net.hollowcube.map.block.handler;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PointOfInterestHandlerMixin extends BlockHandler {

    @NotNull String poiType();

    @Nullable MapVariant requiredVariant();

    @Override
    default void onPlace(@NotNull Placement placement) {
        Player placer = null;
        MapData map;
        var instance = placement.getInstance();
        if (placement instanceof PlayerPlacement pp) {
            map = MapWorld.forPlayer(pp.getPlayer()).map();
            placer = pp.getPlayer();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorld.unsafeFromInstance(placement.getInstance());
            if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) return;
            map = world.map();
        }

        var blockPosition = placement.getBlockPosition();
        boolean added = map.addPointOfInterest(poiType(), blockPosition); //todo: do these atomically
        if (added) {
            if (requiredVariant() != null && map.settings().getVariant() != requiredVariant()) {
                map.settings().setVariant(requiredVariant());
                //todo this whole file is turbo trash code
                if (placer != null) {
                    placer.sendMessage(Component.text("Block was placed which changed the map type to " + requiredVariant()));
                } else {
                    instance.sendMessage(Component.text("Block was placed which changed the map to " + requiredVariant() + " but not by a player we know about :("));
                }
            }
        } else {
            // The player is over the limit, unset the block
            instance.setBlock(blockPosition, Block.AIR);
            var packet = new EffectPacket(2001, blockPosition, placement.getBlock().stateId(), false);
            instance.sendGroupedPacket(packet);

            // If this is a player placement, send a message to the player
            if (placement instanceof PlayerPlacement pp) {
                class Holder {
                    static final Tag<Long> MAX_POI_WARNING = Tag.Long("max_poi_warning").defaultValue(0L);
                    static final long MAX_POI_WARNING_INTERVAL = 1000;
                }
                var player = pp.getPlayer();
                var now = System.currentTimeMillis();
                if (player.getTag(Holder.MAX_POI_WARNING) < now - Holder.MAX_POI_WARNING_INTERVAL) {
                    player.setTag(Holder.MAX_POI_WARNING, now);
                    pp.getPlayer().sendMessage("You have reached the maximum number of points of interest for this map. (todo translation key)");
                }
            }
        }

    }

    @Override
    default void onDestroy(@NotNull Destroy destroy) {
        MapData map;
        if (destroy instanceof PlayerDestroy pd) {
            map = MapWorld.forPlayer(pd.getPlayer()).map();
        } else {
            // OK to choose the first editing world, the block is only placed in editing world.
            var world = MapWorld.unsafeFromInstance(destroy.getInstance());
            if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) return;
            map = world.map();
        }
        map.removePointOfInterest(destroy.getBlockPosition());
    }
}

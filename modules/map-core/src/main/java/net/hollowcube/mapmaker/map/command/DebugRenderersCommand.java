package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.moulberrytweaks.MoulberryTweaksAPI;
import net.hollowcube.compat.moulberrytweaks.debugrender.DebugShape;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderAddPacket;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.ChunkHack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class DebugRenderersCommand {

    public static final Tag<Unit> DEBUG_PLAYER_BOUNDING_BOX = Tag.Transient("mapmaker:debug_player_bounding_box");

    private static final int CHUNK_RANGE = 3;

    private static final Map<String, Integer> MARKER_COLORS = Map.of(
            "mapmaker:checkpoint", 0xd5d5d5,
            "mapmaker:status", 0x7e7e7e,
            "mapmaker:finish", 0xfced59,
            "mapmaker:reset", 0xff0000
    );
    private static final Map<String, Integer> BLOCK_COLORS = Map.of(
            "mapmaker:checkpoint_plate", 0xd5d5d5,
            "mapmaker:status_plate", 0x7e7e7e,
            "mapmaker:finish_plate", 0xfced59
    );

    private static boolean assertMoulberryTweaks(@NotNull Player player) {
        if (!MoulberryTweaksAPI.isPresent(player)) {
            player.sendMessage(Component.translatable("commands.debug.needs_mt"));
            return true;
        }
        return false;
    }

    public static void handleDebugRegions(@NotNull Player player, @NotNull CommandContext ignored) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        if (assertMoulberryTweaks(player)) return;

        // Collect entities
        for (var entity : world.instance().getNearbyEntities(player.getPosition(), 3 * 16)) {
            if (!(entity instanceof MarkerEntity marker)) continue;

            Integer color = MARKER_COLORS.get(marker.getType());
            if (color == null) continue;

            Point min = marker.getMin(), max = marker.getMax();
            if (min == null || max == null) continue;

            var absMin = entity.getPosition().add(min);
            var absMax = entity.getPosition().add(max);
            var center = absMin.add(absMax).mul(0.5f);
            var size = absMax.sub(absMin);

            new ClientboundDebugRenderAddPacket(
                    Key.key("poi", "entity_" + entity.getEntityId()),
                    new DebugShape.Box(center, size, Quaternion.ZERO,
                            color | 0x33000000, color | 0xFF000000, 5),
                    DebugShape.FLAG_SHOW_THROUGH_WALLS, 10 * 20).send(player);
        }

        // Collect ticking block entities
        // This is definitely hacky and gross we should improve it when handling pois better.
        ChunkRange.chunksInRange(player.getPosition(), CHUNK_RANGE, (chunkX, chunkZ) -> {
            var chunk = world.instance().getChunk(chunkX, chunkZ);
            if (chunk == null) return;

            ChunkHack.forEachTickable(chunk, (blockPosition, block) -> {
                var color = BLOCK_COLORS.get(OpUtils.mapOr(block.handler(), bh -> bh.getKey().asString(), ""));
                if (color == null) return;

                // pressure plate bounding box is 14/16, 1/16, 14/16
                var center = blockPosition.add(0.5, 1 / 32.0, 0.5);
                var size = new Vec(14.0 / 16.0, 1.0 / 16.0, 14.0 / 16.0);

                new ClientboundDebugRenderAddPacket(
                        Key.key("poi", "block_" + block.hashCode()),
                        new DebugShape.Box(center, size, Quaternion.ZERO,
                                color | 0x33000000, color | 0xFF000000, 5),
                        DebugShape.FLAG_SHOW_THROUGH_WALLS, 10 * 20).send(player);
            });
        });
    }

    public static void handleDebugBoundingBox(@NotNull Player player, @NotNull CommandContext ignored) {
        if (assertMoulberryTweaks(player)) return;

        if (player.hasTag(DEBUG_PLAYER_BOUNDING_BOX)) {
            player.removeTag(DEBUG_PLAYER_BOUNDING_BOX);
        } else {
            player.setTag(DEBUG_PLAYER_BOUNDING_BOX, Unit.INSTANCE);
        }
    }

    private DebugRenderersCommand() {
    }
}

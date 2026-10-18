package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.util.debug.DebugDraw;
import net.hollowcube.mapmaker.map.util.debug.WireColor;
import net.hollowcube.mapmaker.map.util.debug.DebugShape;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.ChunkHack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;

import static net.hollowcube.common.math.MathConstants.TICKS_PER_SECOND;

public final class DebugRenderersCommand {

    private static final String REGION_PREFIX = "debug_regions/";
    private static final int REGION_LIFETIME = 10 * TICKS_PER_SECOND;

    private static final int CHUNK_RANGE = 3;

    private static final Map<String, WireColor> MARKER_COLORS = Map.of(
            "mapmaker:checkpoint", WireColor.WHITE,
            "mapmaker:status", WireColor.LIGHT_GRAY,
            "mapmaker:finish", WireColor.YELLOW,
            "mapmaker:reset", WireColor.RED
    );
    private static final Map<String, WireColor> BLOCK_COLORS = Map.of(
            "mapmaker:checkpoint_plate", WireColor.WHITE,
            "mapmaker:status_plate", WireColor.LIGHT_GRAY,
            "mapmaker:finish_plate", WireColor.YELLOW
    );
    // The pressure plate collision box, sitting on the floor of its block.
    private static final Vec PLATE_MIN = new Vec(1 / 16.0, 0, 1 / 16.0);
    private static final Vec PLATE_SIZE = new Vec(14 / 16.0, 1 / 16.0, 14 / 16.0);

    public static void handleDebugRegions(@NotNull Player player, @NotNull CommandContext ignored) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        var draw = DebugDraw.of(player);
        draw.clear(REGION_PREFIX);
        for (var entity : world.instance().getNearbyEntities(player.getPosition(), 3 * 16)) {
            if (!(entity instanceof MarkerEntity marker)) continue;

            var color = MARKER_COLORS.get(marker.getType());
            if (color == null) continue;

            Point min = marker.getMin(), max = marker.getMax();
            if (min == null || max == null) continue;

            var a = entity.getPosition().add(min);
            var b = entity.getPosition().add(max);
            draw.draw(REGION_PREFIX + "entity_" + entity.getEntityId(),
                    new DebugShape.Box(a.min(b), a.max(b), color, false, true), REGION_LIFETIME);
        }

        // Collect ticking block entities
        // This is definitely hacky and gross we should improve it when handling pois better.
        ChunkRange.chunksInRange(player.getPosition(), CHUNK_RANGE, (chunkX, chunkZ) -> {
            var chunk = world.instance().getChunk(chunkX, chunkZ);
            if (chunk == null) return;

            ChunkHack.forEachTickable(chunk, (blockPosition, block) -> {
                var color = BLOCK_COLORS.get(OpUtils.mapOr(block.handler(), bh -> bh.getKey().asString(), ""));
                if (color == null) return;

                var min = blockPosition.add(PLATE_MIN);
                draw.draw(REGION_PREFIX + "block_%d_%d_%d".formatted(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ()),
                        new DebugShape.Box(min, min.add(PLATE_SIZE), color, false, true), REGION_LIFETIME);
            });
        });
    }

    private DebugRenderersCommand() {
    }
}

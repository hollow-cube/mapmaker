package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.instance.WorldIndex;
import net.hollowcube.mapmaker.map.util.debug.DebugDraw;
import net.hollowcube.mapmaker.map.util.debug.WireColor;
import net.hollowcube.mapmaker.map.util.debug.DebugShape;
import net.hollowcube.mapmaker.map.util.spatial.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.hollowcube.common.math.MathConstants.TICKS_PER_SECOND;

public final class DebugRenderersCommand {

    private static final String REGION_PREFIX = "debug_regions/";
    private static final int REGION_LIFETIME = 10 * TICKS_PER_SECOND;

    private static final int RANGE = 3 * 16;

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

    public static void handleDebugRegions(@NotNull Player player, @NotNull CommandContext ignored) {
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        var draw = DebugDraw.of(player);
        draw.clear(REGION_PREFIX);

        var position = player.getPosition();
        var query = new BoundingBox(
                (float) (position.x() - RANGE), (float) (position.y() - RANGE), (float) (position.z() - RANGE),
                (float) (position.x() + RANGE), (float) (position.y() + RANGE), (float) (position.z() + RANGE));
        for (var object : world.collisionTree().intersectingObjects(query)) {
            String key;
            WireColor color;
            switch (object) {
                case MarkerEntity marker -> {
                    key = "entity_" + marker.getEntityId();
                    color = MARKER_COLORS.get(marker.getType());
                }
                case WorldIndex.IndexedBlock block -> {
                    var pos = block.position();
                    key = "block_%d_%d_%d".formatted(pos.blockX(), pos.blockY(), pos.blockZ());
                    color = BLOCK_COLORS.get(block.handler().getKey().asString());
                }
                default -> {
                    continue;
                }
            }
            if (color == null) continue;

            var bb = object.boundingBox();
            draw.draw(REGION_PREFIX + key, new DebugShape.Box(
                    new Vec(bb.minX(), bb.minY(), bb.minZ()), new Vec(bb.maxX(), bb.maxY(), bb.maxZ()),
                    color, false, true), REGION_LIFETIME);
        }
    }

    private DebugRenderersCommand() {
    }
}

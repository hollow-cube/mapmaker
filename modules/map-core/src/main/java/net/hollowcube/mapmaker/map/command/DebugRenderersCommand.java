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


import static net.hollowcube.common.math.MathConstants.TICKS_PER_SECOND;

public final class DebugRenderersCommand {

    private static final String REGION_PREFIX = "debug_regions/";
    private static final int REGION_LIFETIME = 10 * TICKS_PER_SECOND;

    private static final int RANGE = 3 * 16;


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
                    color = marker.debugColor();
                }
                case WorldIndex.IndexedBlock block -> {
                    var pos = block.position();
                    key = "block_%d_%d_%d".formatted(pos.blockX(), pos.blockY(), pos.blockZ());
                    color = block.trigger().debugColor();
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

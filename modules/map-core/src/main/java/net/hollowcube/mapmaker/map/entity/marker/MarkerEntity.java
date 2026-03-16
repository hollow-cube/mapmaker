package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.hollowcube.mapmaker.map.util.spatial.SpatialObject;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.metadata.other.MarkerMeta;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a viewable marker entity using axiom or particles to display (depending on the client).
 *
 * <p>Markers are used to add data to a map, such as spawn points, control points, entity spawn locations, etc.
 * Each marker has a namespace ID to describe its type, as well as some associated data dependent on the domain
 * specific use case. Markers can also represent a region with an aabb and have an alias to display in world.</p>
 *
 * <p>todo add a mechanism to handle this on map load (eg in the hub), and in the future with scripting</p>
 */
public class MarkerEntity extends ObjectEntity<MarkerMeta> implements SpatialObject {

    public MarkerEntity() {
        this(UUID.randomUUID());
    }

    public MarkerEntity(@NotNull UUID uuid) {
        super(EntityType.MARKER, uuid);

        this.sendToClient = false;
    }

    public void onPlayerEntered2NoEventTemp(@NotNull Player player) {
        if (this.handler != null) this.handler.onPlayerEnter(player);
    }

    public void onPlayerExited2NoEventTemp(@NotNull Player player) {
        if (this.handler != null) this.handler.onPlayerExit(player);
    }

    @Override
    public @NotNull CompletableFuture<Void> teleport(
            @NotNull Pos position, @NotNull Vec velocity, long @Nullable [] chunks,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flags,
            boolean shouldConfirm
    ) {
        return super.teleport(position, velocity, chunks, flags, shouldConfirm).thenRun(() -> {
            if (handler != null) handler.onPositionChange(position);
            var world = MapWorld.forInstance(instance);
            if (world != null) world.queueCollisionTreeRebuild();
        });
    }

    @Override
    protected void updateBoundingBox() {
        super.updateBoundingBox();
        var world = MapWorld.forInstance(instance);
        if (world != null) world.queueCollisionTreeRebuild();
    }

    @Override
    public @NotNull net.hollowcube.mapmaker.map.util.spatial.BoundingBox boundingBox() {
        var pos = this.getPosition();
        Point min = OpUtils.map(this.getMin(), pos::add), max = OpUtils.map(this.getMax(), pos::add);
        if (min == null || max == null) return net.hollowcube.mapmaker.map.util.spatial.BoundingBox.ZERO;

        var minX = (float) Math.min(min.x(), max.x());
        var minY = (float) Math.min(min.y(), max.y());
        var minZ = (float) Math.min(min.z(), max.z());
        var maxX = (float) Math.max(min.x(), max.x());
        var maxY = (float) Math.max(min.y(), max.y());
        var maxZ = (float) Math.max(min.z(), max.z());

        return new net.hollowcube.mapmaker.map.util.spatial.BoundingBox(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
    }
}

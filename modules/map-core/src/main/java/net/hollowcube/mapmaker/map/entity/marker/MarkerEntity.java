package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityEnteredEvent;
import net.hollowcube.mapmaker.map.event.entity.MarkerEntityExitedEvent;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
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
public class MarkerEntity extends ObjectEntity {
    private static final BoundingBox NO_BB = new BoundingBox(0, 0, 0);

    private final Set<Player> insidePlayers = new HashSet<>();

    public MarkerEntity() {
        this(UUID.randomUUID());
    }

    public MarkerEntity(@NotNull UUID uuid) {
        super(EntityType.MARKER, uuid);

        this.sendToClient = false;
    }

    private void collisionTick() {
        var world = MapWorld.unsafeFromInstance(getInstance());
        if (world == null || world.playWorld() == null) return;
        world = world.playWorld();
        if (world == null || getInstance() != world.instance()) return;

        for (var player : world.players()) {
            if (insidePlayers.contains(player) || !CoordinateUtil.intersects(this, player)) continue;

            insidePlayers.add(player); // Just entered
            world.callEvent(new MarkerEntityEnteredEvent(world, player, this));
            if (this.handler != null) this.handler.onPlayerEnter(player);
        }
        var iter = insidePlayers.iterator();
        while (iter.hasNext()) {
            var player = iter.next();
            if (world.isPlaying(player) && CoordinateUtil.intersects(this, player)) continue;

            iter.remove(); // Just exited
            world.callEvent(new MarkerEntityExitedEvent(world, player, this));
            if (this.handler != null) this.handler.onPlayerExit(player);
        }
    }

    @Override
    public @NotNull CompletableFuture<Void> teleport(
            @NotNull Pos position, @NotNull Vec velocity, long @Nullable [] chunks,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flags,
            boolean shouldConfirm
    ) {
        return super.teleport(position, velocity, chunks, flags, shouldConfirm).thenRun(() -> {
            if (handler != null) handler.onPositionChange(position);
        });
    }

    @Override
    public void update(long time) {
        super.update(time);

        if (getBoundingBox() != NO_BB)
            collisionTick();
    }
}

package net.hollowcube.aj.entity;

import com.google.gson.JsonObject;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.hollowcube.aj.entity.JsonUtil.*;

public class AnimationEntity extends Entity {
    private final UUID nodeId;
    private final String name;
    private final Map<UUID, AnimationEntity> children = new HashMap<>();

    // Mat4 defaultTransform

    private final Pos defaultPosition;

    public AnimationEntity(@NotNull EntityType entityType, @NotNull JsonObject data) {
        super(entityType, UUID.randomUUID());

        hasPhysics = false;
        setNoGravity(true);
        hasCollision = false;
        setSynchronizationTicks(Long.MAX_VALUE);

        this.nodeId = UUID.fromString(data.get("uuid").getAsString());
        this.name = data.get("name").getAsString();

        var transformData = data.getAsJsonObject("default_transform");
        if (transformData != null) {
            // Spawn position + pitch/yaw
            var pos = readVec3(transformData.get("pos"), Vec.ZERO);
            var headRot = readVec2(transformData.get("head_rot"), Vec.ZERO);

            // Display entity transform
            var decomposed = transformData.getAsJsonObject("decomposed");
            var translation = readVec3(decomposed.get("translation"), Vec.ZERO);
            var leftRotation = readVec4(decomposed.get("left_rotation"), EMPTY_FLOATS);
            var rightRotation = readVec4(decomposed.get("right_rotation"), EMPTY_FLOATS);
            var scale = Vec.fromPoint(readVec3(decomposed.get("scale"), Vec.ONE));

            final var meta = getEntityMeta();
            meta.setTranslation(translation);
            meta.setLeftRotation(leftRotation);
//            meta.setRightRotation(rightRotation);
            meta.setScale(scale);

//            this.defaultPosition = new Pos(pos, (float) headRot.x(), (float) headRot.y()).mul(scale);
            this.defaultPosition = Pos.ZERO;
        } else {
            this.defaultPosition = Pos.ZERO;
        }

    }

    public @NotNull UUID nodeId() {
        return this.nodeId;
    }

    public @NotNull String name() {
        return this.name;
    }

    public @NotNull Collection<AnimationEntity> children() {
        return children.values();
    }

    public @Nullable AnimationEntity findNode(@NotNull UUID id) {
        for (var child : children()) {
            if (child.nodeId().equals(id))
                return child;
            var found = child.findNode(id);
            if (found != null)
                return found;
        }
        return null;
    }

    @Override
    public @NotNull AbstractDisplayMeta getEntityMeta() {
        return (AbstractDisplayMeta) super.getEntityMeta();
    }

    @Override
    public @NotNull CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        spawnPosition = spawnPosition.add(defaultPosition);

        var futures = new CompletableFuture<?>[children.size() + 1];
        futures[0] = super.setInstance(instance, spawnPosition);
        var i = 1;
        for (var child : children.values()) {
            futures[i++] = child.setInstance(instance, spawnPosition);
        }

        return CompletableFuture.allOf(futures);
    }

    public void addChild(@NotNull AnimationEntity node) {
        children.put(node.nodeId(), node);
    }
}

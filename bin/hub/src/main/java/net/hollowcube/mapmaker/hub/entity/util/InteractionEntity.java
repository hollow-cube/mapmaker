package net.hollowcube.mapmaker.hub.entity.util;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import org.jetbrains.annotations.NotNull;

public class InteractionEntity extends Entity {

    public interface Target {

        default void beginHover(@NotNull Player player) {
        }

        default void endHover(@NotNull Player player) {
        }

        default void onRightClick(@NotNull Player player) {
        }

    }

    private final Target target;
    private final double interactionDistance;

    public InteractionEntity(int width, int height, double interactionDistance, @NotNull Target target) {
        super(EntityType.INTERACTION);
        this.target = target;
        this.interactionDistance = interactionDistance;

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        final InteractionMeta meta = getEntityMeta();
        meta.setWidth(width);
        meta.setHeight(height);

        setBoundingBox(width, height, width);
    }

    public double interactionDistance() {
        return interactionDistance;
    }

    public @NotNull Target target() {
        return target;
    }

    @Override
    public @NotNull InteractionMeta getEntityMeta() {
        return (InteractionMeta) super.getEntityMeta();
    }

    @Override
    public void tick(long time) {
        // Intentionally do nothing
    }
}

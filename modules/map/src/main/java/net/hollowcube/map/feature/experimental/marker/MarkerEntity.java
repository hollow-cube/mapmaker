package net.hollowcube.map.feature.experimental.marker;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class MarkerEntity extends Entity {
    private final Point origin;

    private final Animation animation;

    public MarkerEntity(@NotNull UUID uniqueId, @NotNull Point origin) {
        super(EntityType.ARMOR_STAND, uniqueId);
        this.origin = origin;

        setCustomName(Component.text("marker-" + displayId()));
        setCustomNameVisible(true);

        setNoGravity(true);

        animation = new Animation(List.of(
//                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 20, 0, 0, 0),
//                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 20, 0, 1, 0),
//                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 20, 0, 0, 0),
//                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 20, 0, -1, 0),
//                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 20, 0, 0, 0)

                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_OUT_SINE, 60, 0, 0, 0),
                new Animation.Keyframe(Animation.EasingFunc.LINEAR, 10, 0, 4, 0),
                new Animation.Keyframe(Animation.EasingFunc.EASE_IN_QUINT, 5, 0, 0, 0)
        ), this);
    }

    /**
     * Returns a readable ID for this marker. The internal ID is longer
     */
    public @NotNull String displayId() {
        return getUuid().toString().substring(0, 6);
    }

    public @NotNull Point origin() {
        return origin;
    }

    @Override
    public void tick(long time) {
        super.tick(time);
    }

    @Override
    public void update(long time) {
        super.update(time);

        animation.tick();
//        refreshPosition(getPosition().add(0, 0.1, 0), true);
    }
}

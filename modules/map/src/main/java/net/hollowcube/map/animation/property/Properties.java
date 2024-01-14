package net.hollowcube.map.animation.property;

import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;

public final class Properties {

    public static final Property<Pos> POSITION = new Property<>("position", Pos.ZERO, false, CoordinateUtil::lerp, Entity::teleport);

    public static final Property<Vec> SCALE = new Property<>("scale", Vec.ONE, false, CoordinateUtil::lerp, (entity, scale) -> {
        if (entity.getEntityMeta() instanceof AbstractDisplayMeta meta) {
            meta.setScale(scale);
        }
    });
}

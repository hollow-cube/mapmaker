package net.hollowcube.map2.entity.impl;

import net.hollowcube.map2.entity.MapEntity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DisplayEntity extends MapEntity {
    public DisplayEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);

        setNoGravity(true);
        hasPhysics = false;
    }
}

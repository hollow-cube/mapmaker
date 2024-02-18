package net.hollowcube.mapmaker.map.entity.impl;

import net.hollowcube.mapmaker.map.entity.MapEntity;
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

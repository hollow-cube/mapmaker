package net.hollowcube.mapmaker.map.entity.impl.living;

import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractAgeableEntity extends AbstractMobEntity {

    protected AbstractAgeableEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractAgeableEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

}

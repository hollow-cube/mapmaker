package net.hollowcube.mapmaker.map.entity.impl.living;

import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractMobEntity extends AbstractLivingEntity {

    public AbstractMobEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    public AbstractMobEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

}

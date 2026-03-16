package net.hollowcube.mapmaker.map.entity.impl.base;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.entity.metadata.MobMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractMobEntity<M extends MobMeta> extends AbstractLivingEntity<M> {

    public AbstractMobEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    public AbstractMobEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

}

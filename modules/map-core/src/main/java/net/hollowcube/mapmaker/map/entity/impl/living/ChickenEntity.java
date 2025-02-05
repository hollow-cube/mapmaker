package net.hollowcube.mapmaker.map.entity.impl.living;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.ChickenMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChickenEntity extends AbstractAgeableEntity {

    public ChickenEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType);
    }

    @Override
    public @NotNull ChickenMeta getEntityMeta() {
        return (ChickenMeta) super.getEntityMeta();
    }

}

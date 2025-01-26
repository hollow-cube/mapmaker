package net.hollowcube.mapmaker.map.entity.impl.living;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.PandaMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PandaEntity extends AbstractAgeableEntity {

    public PandaEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public @NotNull PandaMeta getEntityMeta() {
        return (PandaMeta) super.getEntityMeta();
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);
    }

}

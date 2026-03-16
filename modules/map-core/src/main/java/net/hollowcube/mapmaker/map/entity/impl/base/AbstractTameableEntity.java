package net.hollowcube.mapmaker.map.entity.impl.base;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.AgeableMobMeta;
import net.minestom.server.entity.metadata.animal.tameable.TameableAnimalMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractTameableEntity<M extends TameableAnimalMeta> extends AbstractAgeableEntity<M> {

    public static final MapEntityInfo<@NotNull AbstractTameableEntity<? extends AgeableMobMeta>> INFO = MapEntityInfo.<AbstractTameableEntity<? extends AgeableMobMeta>>builder(AbstractAgeableEntity.INFO)
        .with("Is Tamed", MapEntityInfoType.Bool(false, TameableAnimalMeta::setTamed, TameableAnimalMeta::isTamed))
        .with("Is Sitting", MapEntityInfoType.Bool(false, TameableAnimalMeta::setSitting, TameableAnimalMeta::isSitting))
        .build();

    private static final String SITTING_KEY = "Sitting";
    private static final String TAMED_KEY = "mapmaker:tamed";

    protected AbstractTameableEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractTameableEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setSitting(tag.getBoolean(SITTING_KEY, false));
        this.getEntityMeta().setTamed(tag.getBoolean(TAMED_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(SITTING_KEY, this.getEntityMeta().isSitting());
        tag.putBoolean(TAMED_KEY, this.getEntityMeta().isTamed());
    }
}

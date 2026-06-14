package net.hollowcube.mapmaker.map.entity.impl.animal;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.FoxMeta;

import java.util.UUID;

public class FoxEntity extends AbstractAgeableEntity<FoxMeta> {

    public static final MapEntityInfo<FoxEntity> INFO = MapEntityInfo.<FoxEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(FoxMeta.Variant.class, FoxMeta.Variant.RED, DataComponents.FOX_VARIANT))
        .with("Sitting", MapEntityInfoType.Bool(false, FoxMeta::setSitting, FoxMeta::isSitting))
        .with("Sleeping", MapEntityInfoType.Bool(false, FoxMeta::setSleeping, FoxMeta::isSleeping))
        .build();

    private static final String TYPE_KEY = "Type";
    private static final String CROUCHING_KEY = "Crouching";
    private static final String SITTING_KEY = "Sitting";
    private static final String SLEEPING_KEY = "Sleeping";

    public FoxEntity(UUID uuid) {
        super(EntityType.FOX, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.FOX_VARIANT, NbtUtilV2.readStringEnum(tag.get(TYPE_KEY), FoxMeta.Variant.class));
        this.getEntityMeta().setFoxSneaking(tag.getBoolean(CROUCHING_KEY, false));
        this.getEntityMeta().setSitting(tag.getBoolean(SITTING_KEY, false));
        this.getEntityMeta().setSleeping(tag.getBoolean(SLEEPING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(TYPE_KEY, NbtUtilV2.writeStringEnum(this.get(DataComponents.FOX_VARIANT, FoxMeta.Variant.RED)));
        tag.putBoolean(CROUCHING_KEY, this.getEntityMeta().isFoxSneaking());
        tag.putBoolean(SITTING_KEY, this.getEntityMeta().isSitting());
        tag.putBoolean(SLEEPING_KEY, this.getEntityMeta().isSleeping());
    }
}

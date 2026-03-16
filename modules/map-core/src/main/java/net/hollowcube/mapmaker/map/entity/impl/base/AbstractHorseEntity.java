package net.hollowcube.mapmaker.map.entity.impl.base;

import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.AbstractHorseMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class AbstractHorseEntity<M extends AbstractHorseMeta> extends AbstractAgeableEntity<M> {

    public static final MapEntityInfo<@NotNull AbstractHorseEntity<? extends AbstractHorseMeta>> INFO = MapEntityInfo.<AbstractHorseEntity<? extends AbstractHorseMeta>>builder(AbstractAgeableEntity.INFO)
        .with("Saddled", CommonMapEntityInfoTypes.Saddle())
        .build();

    public static final MapEntityInfo<@NotNull AbstractHorseEntity<? extends AbstractHorseMeta>> ARMORED_INFO = MapEntityInfo.<AbstractHorseEntity<? extends AbstractHorseMeta>>builder(AbstractAgeableEntity.INFO)
        .with("Saddled", CommonMapEntityInfoTypes.Saddle())
        .with("Armor", CommonMapEntityInfoTypes.BodyArmor(CommonMapEntityInfoTypes.HorseArmor.class))
        .build();

    private static final String EATING_HAYSTACK_KEY = "EatingHaystack";
    private static final String BRED_KEY = "Bred";
    private static final String TAME_KEY = "Tame";
    private static final String STANDING_KEY = "mapmaker:standing";

    protected AbstractHorseEntity(@NotNull EntityType entityType) {
        super(entityType);
    }

    protected AbstractHorseEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    @Override
    public void readData(@NotNull CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setEating(tag.getBoolean(EATING_HAYSTACK_KEY, false));
        this.getEntityMeta().setHasBred(tag.getBoolean(BRED_KEY, false));
        this.getEntityMeta().setTamed(tag.getBoolean(TAME_KEY, false));

        // Mapmaker
        this.getEntityMeta().setRearing(tag.getBoolean(STANDING_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.@NotNull Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putBoolean(EATING_HAYSTACK_KEY, this.getEntityMeta().isEating());
        tag.putBoolean(BRED_KEY, this.getEntityMeta().isHasBred());
        tag.putBoolean(TAME_KEY, this.getEntityMeta().isTamed());

        // Mapmaker
        tag.putBoolean(STANDING_KEY, this.getEntityMeta().isRearing());
    }

}

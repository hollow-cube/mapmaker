package net.hollowcube.mapmaker.map.entity.impl.villager;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.*;
import net.minestom.server.entity.metadata.villager.VillagerMeta;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.hollowcube.mapmaker.map.entity.impl.villager.VillagerDataUtils.ProfessionInfoType;

public class VillagerEntity extends AbstractVillagerEntity<VillagerMeta> {

    public static final MapEntityInfo<VillagerEntity> INFO = MapEntityInfo.<VillagerEntity>builder(AbstractAgeableEntity.INFO)
        .with("Type", VillagerData(VillagerType.class, VillagerType.PLAINS, VillagerMeta.VillagerData::withType, VillagerMeta.VillagerData::type))
        .with("Level", VillagerData(VillagerMeta.Level.class, VillagerMeta.Level.NOVICE, VillagerMeta.VillagerData::withLevel, VillagerMeta.VillagerData::level))
        .with("Profession", ProfessionInfoType(
            (meta, profession) -> meta.setVillagerData(meta.getVillagerData().withProfession(profession)),
            (meta) -> meta.getVillagerData().profession()
        ))
        .build();

    private static final String VILLAGER_DATA_KEY = "VillagerData";

    public VillagerEntity(UUID uuid) {
        super(EntityType.VILLAGER, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setVillagerData(VillagerDataUtils.read(tag.get(VILLAGER_DATA_KEY), VillagerMeta.VillagerData.DEFAULT));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        var villagerDataTag = VillagerDataUtils.write(this.getEntityMeta().getVillagerData());
        if (villagerDataTag != null) tag.put(VILLAGER_DATA_KEY, villagerDataTag);
    }

    private static <T extends Enum<T>> MapEntityInfoType<T, VillagerEntity> VillagerData(
        Class<T> type,
        T fallback,
        BiFunction<VillagerMeta.VillagerData, T, VillagerMeta.VillagerData> setter,
        Function<VillagerMeta.VillagerData, T> getter
    ) {
        return MapEntityInfoType.Enum(
            type,
            fallback,
            (meta, value) -> meta.setVillagerData(setter.apply(meta.getVillagerData(), value)),
            (meta) -> getter.apply(meta.getVillagerData())
        );
    }
}

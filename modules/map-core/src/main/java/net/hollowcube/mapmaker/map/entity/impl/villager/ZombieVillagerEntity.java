package net.hollowcube.mapmaker.map.entity.impl.villager;

import net.hollowcube.mapmaker.map.entity.impl.hostile.zombie.AbstractZombieEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.VillagerType;
import net.minestom.server.entity.metadata.monster.zombie.ZombieVillagerMeta;
import net.minestom.server.entity.metadata.villager.VillagerMeta;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.hollowcube.mapmaker.map.entity.impl.villager.VillagerDataUtils.ProfessionInfoType;

public class ZombieVillagerEntity extends AbstractZombieEntity<ZombieVillagerMeta> {

    public static final MapEntityInfo<ZombieVillagerEntity> INFO = MapEntityInfo.<ZombieVillagerEntity>builder(AbstractZombieEntity.INFO)
        .with("Is Converting", MapEntityInfoType.Bool(false, ZombieVillagerMeta::setConverting, ZombieVillagerMeta::isConverting))
        .with("Type", VillagerData(VillagerType.class, VillagerType.PLAINS, VillagerMeta.VillagerData::withType, VillagerMeta.VillagerData::type))
        .with("Level", VillagerData(VillagerMeta.Level.class, VillagerMeta.Level.NOVICE, VillagerMeta.VillagerData::withLevel, VillagerMeta.VillagerData::level))
        .with("Profession", ProfessionInfoType(
            (meta, profession) -> meta.setVillagerData(meta.getVillagerData().withProfession(profession)),
            (meta) -> meta.getVillagerData().profession()
        ))
        .build();

    private static final String CONVERSION_TIME_KEY = "ConversionTime";
    private static final String VILLAGER_DATA_KEY = "VillagerData";

    public ZombieVillagerEntity(UUID uuid) {
        super(EntityType.ZOMBIE_VILLAGER, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.getEntityMeta().setBecomingDrowned(tag.getInt(CONVERSION_TIME_KEY, -1) != -1);
//        this.getEntityMeta().setVillagerData(VillagerDataCodec.read(tag.get(VILLAGER_DATA_KEY), VillagerMeta.VillagerData.DEFAULT));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putInt(CONVERSION_TIME_KEY, this.getEntityMeta().isConverting() ? 0 : -1);
        var villagerDataTag = VillagerDataUtils.write(this.getEntityMeta().getVillagerData());
        if (villagerDataTag != null) tag.put(VILLAGER_DATA_KEY, villagerDataTag);
    }

    private static <T extends Enum<T>> MapEntityInfoType<T, ZombieVillagerEntity> VillagerData(
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

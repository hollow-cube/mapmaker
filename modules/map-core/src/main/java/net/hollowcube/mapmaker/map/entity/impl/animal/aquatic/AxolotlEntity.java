package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractAgeableEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.water.AxolotlMeta;

import java.util.UUID;

public class AxolotlEntity extends AbstractAgeableEntity<AxolotlMeta> {

    public static final MapEntityInfo<AxolotlEntity> INFO = MapEntityInfo.<AxolotlEntity>builder(AbstractAgeableEntity.INFO)
        .with("Variant", MapEntityInfoType.Enum(AxolotlMeta.Variant.class, AxolotlMeta.Variant.LUCY, DataComponents.AXOLOTL_VARIANT))
        .with("Playing Dead", MapEntityInfoType.Bool(false, AxolotlMeta::setPlayingDead, AxolotlMeta::isPlayingDead))
        .build();

    private static final String PLAYING_DEAD_KEY = "mapmaker:playing_dead";
    private static final String VARIANT_KEY = "Variant";

    public AxolotlEntity(UUID uuid) {
        super(EntityType.AXOLOTL, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.AXOLOTL_VARIANT, NbtUtilV2.readIntEnum(tag.get(VARIANT_KEY), AxolotlMeta.Variant.class));

        // Mapmaker
        this.getEntityMeta().setPlayingDead(tag.getBoolean(PLAYING_DEAD_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.put(VARIANT_KEY, NbtUtilV2.writeIntEnum(this.get(DataComponents.AXOLOTL_VARIANT, AxolotlMeta.Variant.LUCY)));

        // Mapmaker
        tag.putBoolean(PLAYING_DEAD_KEY, this.getEntityMeta().isPlayingDead());
    }
}

package net.hollowcube.mapmaker.map.entity.impl.hostile.illager;

import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.monster.raider.WitchMeta;

import java.util.UUID;

public class WitchEntity extends AbstractIllagerEntity<WitchMeta> {

    public static final MapEntityInfo<WitchEntity> INFO = MapEntityInfo.<WitchEntity>builder(AbstractIllagerEntity.INFO)
        .with("Drinking Potion", MapEntityInfoType.Bool(false, WitchMeta::setDrinkingPotion, WitchMeta::isDrinkingPotion))
        .build();

    private static final String DRINKING_POTION_KEY = "mapmaker:is_drinking_potion";

    public WitchEntity(UUID uuid) {
        super(EntityType.WITCH, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Mapmaker
        this.getEntityMeta().setDrinkingPotion(tag.getBoolean(DRINKING_POTION_KEY, false));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Mapmaker
        tag.putBoolean(DRINKING_POTION_KEY, this.getEntityMeta().isDrinkingPotion());
    }
}

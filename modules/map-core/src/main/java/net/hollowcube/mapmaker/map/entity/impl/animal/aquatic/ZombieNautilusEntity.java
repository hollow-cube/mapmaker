package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractTameableEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.hollowcube.mapmaker.map.util.NbtUtilV2;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.ZombieNautilusMeta;
import net.minestom.server.entity.metadata.animal.ZombieNautilusVariant;
import net.minestom.server.registry.Registries;

import java.util.UUID;

public class ZombieNautilusEntity extends AbstractTameableEntity<ZombieNautilusMeta> {

    public static final MapEntityInfo<ZombieNautilusEntity> INFO = MapEntityInfo.<ZombieNautilusEntity>builder(AbstractTameableEntity.INFO)
        .with("Variant", MapEntityInfoType.RegisteredKey(Registries::zombieNautilusVariant, ZombieNautilusVariant.TEMPERATE, DataComponents.ZOMBIE_NAUTILUS_VARIANT))
        .with("Armor", CommonMapEntityInfoTypes.BodyArmor(CommonMapEntityInfoTypes.NautilusArmor.class))
        .build();

    private static final String VARIANT_KEY = "variant";

    public ZombieNautilusEntity(UUID uuid) {
        super(EntityType.ZOMBIE_NAUTILUS, uuid);
    }

    @Override
    public void readData(CompoundBinaryTag tag) {
        super.readData(tag);

        // Vanilla
        this.set(DataComponents.ZOMBIE_NAUTILUS_VARIANT, NbtUtilV2.readRegistryKey(tag.get(VARIANT_KEY), Registries::zombieNautilusVariant, ZombieNautilusVariant.TEMPERATE));
    }

    @Override
    public void writeData(CompoundBinaryTag.Builder tag) {
        super.writeData(tag);

        // Vanilla
        tag.putString(VARIANT_KEY, this.get(DataComponents.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusVariant.TEMPERATE).name());
    }
}

package net.hollowcube.mapmaker.map.entity.impl.animal.aquatic;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractTameableEntity;
import net.hollowcube.mapmaker.map.entity.info.CommonMapEntityInfoTypes;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfo;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.animal.NautilusMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NautilusEntity extends AbstractTameableEntity<NautilusMeta> {

    public static final MapEntityInfo<@NotNull NautilusEntity> INFO = MapEntityInfo.<NautilusEntity>builder(AbstractTameableEntity.INFO)
        .with("Armor", CommonMapEntityInfoTypes.BodyArmor(CommonMapEntityInfoTypes.NautilusArmor.class))
        .build();

    public NautilusEntity(@NotNull UUID uuid) {
        super(EntityType.NAUTILUS, uuid);
    }
}

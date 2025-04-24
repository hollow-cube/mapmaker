package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3689 extends DataVersion {
    public V3689() {
        super(3689);

        addReference(DataTypes.ENTITY, "minecraft:breeze");
        addReference(DataTypes.ENTITY, "minecraft:wind_charge");
        addReference(DataTypes.ENTITY, "minecraft:breeze_wind_charge");

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:trial_spawner", field -> field
                // todo
                .list("spawn_potentials.data.entity", DataTypes.ENTITY)
                .single("spawn_data.entity", DataTypes.ENTITY));
    }

}

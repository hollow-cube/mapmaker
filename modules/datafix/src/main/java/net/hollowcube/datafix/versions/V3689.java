package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3689 extends DataVersion {
    public V3689() {
        super(3689);

        addReference(DataType.ENTITY, "minecraft:breeze");
        addReference(DataType.ENTITY, "minecraft:wind_charge");
        addReference(DataType.ENTITY, "minecraft:breeze_wind_charge");

        addReference(DataType.BLOCK_ENTITY, "minecraft:trial_spawner", field -> field
                // todo
                .list("spawn_potentials.data.entity", DataType.ENTITY_TREE)
                .single("spawn_data.entity", DataType.ENTITY_TREE));
    }

}

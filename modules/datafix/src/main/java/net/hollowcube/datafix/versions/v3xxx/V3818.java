package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3818 extends DataVersion {
    public V3818() {
        super(3818);

        addReference(DataType.BLOCK_ENTITY, "minecraft:beehive", field -> field
                // todo
                .list("bees.entity_data", DataType.ENTITY_TREE));

        addFix(DataType.BLOCK_ENTITY, "minecraft:beehive", V3818::fixBeehiveBlockEntityNames);
    }

    private static Value fixBeehiveBlockEntityNames(Value blockEntity) {
        blockEntity.put("bees", blockEntity.remove("Bees"));

        for (var bee : blockEntity.get("bees")) {
            bee.put("entity_data", bee.remove("EntityData"));
            bee.put("ticks_in_hive", bee.remove("TicksInHive"));
            bee.put("min_ticks_in_hive", bee.remove("MinOccupationTicks"));
        }

        return null;
    }

}

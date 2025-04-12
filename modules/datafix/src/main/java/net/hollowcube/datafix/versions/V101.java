package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class V101 extends DataVersion {

    public V101() {
        super(101);

        addFix(DataType.ENTITY, "Villager", this::fixCanPickUpLoot);
        addFix(DataType.ITEM_STACK, "minecraft:stone", o -> {
            o.put("id", "minecraft:stone2");
            return o;
        });
    }

    private @NotNull Map<String, Object> fixCanPickUpLoot(@NotNull Map<String, Object> entity) {
        entity.put("CanPickUpLoot", true);
        return entity;
    }
}

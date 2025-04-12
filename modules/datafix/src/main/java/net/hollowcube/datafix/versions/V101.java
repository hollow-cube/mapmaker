package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

public class V101 extends DataVersion {

    public V101() {
        super(101);

        addFix(DataType.ENTITY, "Villager", V101::fixCanPickUpLoot);
    }

    private static @NotNull Value fixCanPickUpLoot(@NotNull Value entity) {
        entity.put("CanPickUpLoot", true);
        return entity;
    }
}

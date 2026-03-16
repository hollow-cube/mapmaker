package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataFix;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class V3813 extends DataVersion {
    private static final Set<String> PATROLLING_MOBS = Set.of(
        "minecraft:witch", "minecraft:ravager",
        "minecraft:pillager", "minecraft:illusioner",
        "minecraft:evoker", "minecraft:vindicator"
    );
    private static final Map<String, String> PATROLLING_MOB_FIELDS = Map.of(
        "PatrolTarget", "patrol_target"
    );

    public V3813() {
        super(3813);

        addFix(DataTypes.ENTITY, posFieldFixAndRename(Map.of("Leash", "leash")));
        addFix(DataTypes.ENTITY, "minecraft:bee", posFieldFixAndRename(
            Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")));
        addFix(DataTypes.ENTITY, "minecraft:end_crystal", posFieldFixAndRename(
            Map.of("BeamTarget", "beam_target")));
        addFix(DataTypes.ENTITY, "minecraft:wandering_trader", posFieldFixAndRename(
            Map.of("WanderTarget", "wander_target")));
        PATROLLING_MOBS.forEach(id -> addFix(DataTypes.ENTITY, id, posFieldFixAndRename(PATROLLING_MOB_FIELDS)));

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:beehive", posFieldFixAndRename(Map.of("FlowerPos", "flower_pos")));
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:end_gateway", posFieldFixAndRename(Map.of("ExitPortal", "exit_portal")));

        addFix(DataTypes.ITEM_STACK, "minecraft:compass", V3813::fixCompassItemStack);
    }

    private static DataFix posFieldFixAndRename(Map<String, String> fields) {
        return value -> {
            fields.forEach((oldField, newField) ->
                    value.put(newField, fixBlockPos(value.remove(oldField))));
            return null;
        };
    }

    private static @Nullable Value fixCompassItemStack(Value itemStack) {
        var tag = itemStack.get("tag");
        if (!tag.isMapLike()) return null;

        tag.put("LodestonePos", fixBlockPos(tag.get("LodestonePos")));
        return null;
    }

    private static Value fixBlockPos(Value value) {
        Number x = value.get("x").as(Number.class, null);
        Number y = value.get("y").as(Number.class, null);
        Number z = value.get("z").as(Number.class, null);
        if (x == null || y == null || z == null) return value;
        return Value.wrap(new int[]{x.intValue(), y.intValue(), z.intValue()});
    }
}

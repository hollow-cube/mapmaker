package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4314 extends DataVersion {

    public V4314() {
        super(4314);

        addFix(DataTypes.ENTITY, V4314::fixLivingEntity);
        addFix(DataTypes.ENTITY, "minecraft:vex", V4314::fixVex);
        addFix(DataTypes.ENTITY, "minecraft:phantom", V4314::fixPhantom);
        addFix(DataTypes.ENTITY, "minecraft:turtle", V4314::fixTurtle);
        addFix(DataTypes.ENTITY, "minecraft:item_frame", V4314::fixBlockAttached);
        addFix(DataTypes.ENTITY, "minecraft:glow_item_frame", V4314::fixBlockAttached);
        addFix(DataTypes.ENTITY, "minecraft:painting", V4314::fixBlockAttached);
        addFix(DataTypes.ENTITY, "minecraft:leash_knot", V4314::fixBlockAttached);
    }

    private static @Nullable Value fixLivingEntity(Value entity) {
        fixInlineBlockPos(entity, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
        return null;
    }

    private static @Nullable Value fixVex(Value entity) {
        entity.put("life_ticks", entity.remove("LifeTicks"));
        fixInlineBlockPos(entity, "BoundX", "BoundY", "BoundZ", "bound_pos");
        return null;
    }

    private static @Nullable Value fixPhantom(Value entity) {
        entity.put("size", entity.remove("Size"));
        fixInlineBlockPos(entity, "AX", "AY", "AZ", "anchor_pos");
        return null;
    }

    private static @Nullable Value fixTurtle(Value entity) {
        entity.remove("TravelPosX");
        entity.remove("TravelPosY");
        entity.remove("TravelPosZ");
        entity.put("has_egg", entity.remove("HasEgg"));
        fixInlineBlockPos(entity, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
        return null;
    }

    private static @Nullable Value fixBlockAttached(Value entity) {
        fixInlineBlockPos(entity, "TileX", "TileY", "TileZ", "block_pos");
        return null;
    }

    private static void fixInlineBlockPos(Value value, String xField, String yField, String zField, String inlineField) {
        var x = value.remove(xField).as(Number.class, null);
        var y = value.remove(yField).as(Number.class, null);
        var z = value.remove(zField).as(Number.class, null);
        if (x == null || y == null || z == null) return;
        value.put(inlineField, new int[]{x.intValue(), y.intValue(), z.intValue()});
    }

}

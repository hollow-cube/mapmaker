package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;

public class V4299 extends DataVersion {

    public V4299() {
        super(4299);

        addFix(DataTypes.ITEM_STACK, "minecraft:salmon_bucket", V4299::fixSpawnerItemVariantSalmonBucket);
        addFix(DataTypes.ITEM_STACK, "minecraft:axolotl_bucket", V4299::fixSpawnerItemVariantAxolotlBucket);
        addFix(DataTypes.ITEM_STACK, "minecraft:tropical_fish_bucket", V4299::fixSpawnerItemVariantTropicalFishBucket);
        addFix(DataTypes.ITEM_STACK, "minecraft:painting", V4299::fixSpawnerItemVariantPainting);
    }

    private static Value fixSpawnerItemVariantSalmonBucket(Value itemStack) {
        var bucketEntityData = itemStack.get("components").get("minecraft:bucket_entity_data");
        if (!bucketEntityData.isMapLike()) return null;
        bucketEntityData.put("minecraft:salmon/size", bucketEntityData.remove("type"));
        return null;
    }

    private static Value fixSpawnerItemVariantAxolotlBucket(Value itemStack) {
        var bucketEntityData = itemStack.get("components").get("minecraft:bucket_entity_data");
        if (!bucketEntityData.isMapLike()) return null;
        var variant = bucketEntityData.remove("Variant").as(Number.class, null);
        if (variant == null) return null;
        bucketEntityData.put("minecraft:axolotl/variant", switch (variant.intValue()) {
            case 1 -> "wild";
            case 2 -> "gold";
            case 3 -> "cyan";
            case 4 -> "blue";
            default -> "lucy";
        });
        return null;
    }

    private static Value fixSpawnerItemVariantTropicalFishBucket(Value itemStack) {
        var bucketEntityData = itemStack.get("components").get("minecraft:bucket_entity_data");
        if (!bucketEntityData.isMapLike()) return null;
        var variant = bucketEntityData.remove("BucketVariantTag").as(Number.class, null);
        if (variant == null) return null;
        bucketEntityData.put("minecraft:tropical_fish/pattern", switch (variant.intValue() & 65535) {
            case 1 -> "flopper";
            case 256 -> "sunstreak";
            case 257 -> "stripey";
            case 512 -> "snooper";
            case 513 -> "glitter";
            case 768 -> "dasher";
            case 769 -> "blockfish";
            case 1024 -> "brinely";
            case 1025 -> "betty";
            case 1280 -> "spotty";
            case 1281 -> "clayfish";
            default -> "kob";
        });
        bucketEntityData.put("minecraft:tropical_fish/base_color", DataFixUtils.dyeColorIdToName(variant.intValue() >> 16 & 0xFF));
        bucketEntityData.put("minecraft:tropical_fish/pattern_color", DataFixUtils.dyeColorIdToName(variant.intValue() >> 24 & 0xFF));
        return null;
    }

    private static Value fixSpawnerItemVariantPainting(Value itemStack) {
        var components = itemStack.get("components");
        var entityData = components.get("minecraft:entity_data");
        if (!entityData.isMapLike() || "minecraft:painting".equals(entityData.getValue("id")))
            return null;
        entityData.remove("id");
        var variant = entityData.remove("variant");
        if (entityData.size(0) == 0)
            components.remove("minecraft:entity_data");
        if (!variant.isNull())
            components.put("minecraft:painting/variant", variant);
        return null;
    }

}

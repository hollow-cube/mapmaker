package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4054 extends DataVersion {

    private static final String OMINOUS_BANNER_TRANSLATION = "block.minecraft.ominous_banner";

    public V4054() {
        super(4054);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:banner", V4054::fixOminousBannerRarity);
        addFix(DataTypes.ITEM_STACK, "minecraft:white_banner", V4054::fixOminousBannerRarity);
    }

    private static @Nullable Value fixOminousBannerRarity(Value value) {
        var components = value.get("components");
        var itemName = components.get("minecraft:item_name");
        boolean isOminousBanner = itemName.toString().contains(OMINOUS_BANNER_TRANSLATION);
        if (!isOminousBanner) return null;

        components.put("minecraft:item_name", "{\"translate\":\"" + OMINOUS_BANNER_TRANSLATION + "\"}");
        components.put("minecraft:rarity", "uncommon");
        return null;
    }
}

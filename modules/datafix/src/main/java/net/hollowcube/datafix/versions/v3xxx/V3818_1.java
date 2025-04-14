package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V3818_1 extends DataVersion {
    private static final Map<String, String> PATTERN_RENAMES;

    public V3818_1() {
        super(3818, 1);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:banner", V3818_1::fixBannerPatternNames);
    }

    private static Value fixBannerPatternNames(Value blockEntity) {
        blockEntity.put("patterns", blockEntity.remove("Patterns"));

        for (var layer : blockEntity.get("patterns")) {
            var pattern = layer.get("Pattern").as(String.class, "b");
            layer.put("pattern", PATTERN_RENAMES.getOrDefault(pattern, pattern));

            var color = layer.remove("Color").as(Number.class, 0).intValue();
            layer.put("color", DataFixUtils.dyeColorIdToName(color));
        }

        return null;
    }

    static {
        PATTERN_RENAMES = Map.ofEntries(
                Map.entry("b", "minecraft:base"),
                Map.entry("bl", "minecraft:square_bottom_left"),
                Map.entry("br", "minecraft:square_bottom_right"),
                Map.entry("tl", "minecraft:square_top_left"),
                Map.entry("tr", "minecraft:square_top_right"),
                Map.entry("bs", "minecraft:stripe_bottom"),
                Map.entry("ts", "minecraft:stripe_top"),
                Map.entry("ls", "minecraft:stripe_left"),
                Map.entry("rs", "minecraft:stripe_right"),
                Map.entry("cs", "minecraft:stripe_center"),
                Map.entry("ms", "minecraft:stripe_middle"),
                Map.entry("drs", "minecraft:stripe_downright"),
                Map.entry("dls", "minecraft:stripe_downleft"),
                Map.entry("ss", "minecraft:small_stripes"),
                Map.entry("cr", "minecraft:cross"),
                Map.entry("sc", "minecraft:straight_cross"),
                Map.entry("bt", "minecraft:triangle_bottom"),
                Map.entry("tt", "minecraft:triangle_top"),
                Map.entry("bts", "minecraft:triangles_bottom"),
                Map.entry("tts", "minecraft:triangles_top"),
                Map.entry("ld", "minecraft:diagonal_left"),
                Map.entry("rd", "minecraft:diagonal_up_right"),
                Map.entry("lud", "minecraft:diagonal_up_left"),
                Map.entry("rud", "minecraft:diagonal_right"),
                Map.entry("mc", "minecraft:circle"),
                Map.entry("mr", "minecraft:rhombus"),
                Map.entry("vh", "minecraft:half_vertical"),
                Map.entry("hh", "minecraft:half_horizontal"),
                Map.entry("vhr", "minecraft:half_vertical_right"),
                Map.entry("hhb", "minecraft:half_horizontal_bottom"),
                Map.entry("bo", "minecraft:border"),
                Map.entry("cbo", "minecraft:curly_border"),
                Map.entry("gra", "minecraft:gradient"),
                Map.entry("gru", "minecraft:gradient_up"),
                Map.entry("bri", "minecraft:bricks"),
                Map.entry("glb", "minecraft:globe"),
                Map.entry("cre", "minecraft:creeper"),
                Map.entry("sku", "minecraft:skull"),
                Map.entry("flo", "minecraft:flower"),
                Map.entry("moj", "minecraft:mojang"),
                Map.entry("pig", "minecraft:piglin")
        );
    }
}

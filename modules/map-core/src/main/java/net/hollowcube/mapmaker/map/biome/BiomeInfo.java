package net.hollowcube.mapmaker.map.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.kyori.adventure.key.Key;
import net.minestom.server.color.Color;
import net.minestom.server.item.Material;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BiomeInfo {

    private static final Color DEFAULT_SKY_COLOR = new Color(0x78A7FF);
    private static final Color DEFAULT_FOG_COLOR = new Color(0xC0D8FF);
    private static final Color DEFAULT_WATER_COLOR = new Color(0x3F76E4);
    private static final Color DEFAULT_WATER_FOG_COLOR = new Color(0x050533);

    public static final Codec<BiomeInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(BiomeInfo::getName),
            ExtraCodecs.MATERIAL.lenientOptionalFieldOf("displayItem", Material.GRASS_BLOCK).forGetter(BiomeInfo::getDisplayItem),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("skyColor", DEFAULT_SKY_COLOR).forGetter(BiomeInfo::getSkyColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("fogColor", DEFAULT_FOG_COLOR).forGetter(BiomeInfo::getFogColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("waterColor", DEFAULT_WATER_COLOR).forGetter(BiomeInfo::getWaterColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("waterFogColor", DEFAULT_WATER_FOG_COLOR).forGetter(BiomeInfo::getWaterFogColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("grassColor").forGetter(ExtraCodecs.optional(BiomeInfo::getGrassColor)),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("foliageColor").forGetter(ExtraCodecs.optional(BiomeInfo::getFoliageColor))
    ).apply(i, BiomeInfo::new));

    private String name = "";
    private Material displayItem = Material.GRASS_BLOCK;

    private Object particle = null; //todo

    private Color skyColor = DEFAULT_SKY_COLOR;
    private Color fogColor = DEFAULT_FOG_COLOR;
    private Color waterColor = DEFAULT_WATER_COLOR;
    private Color waterFogColor = DEFAULT_WATER_FOG_COLOR;
    private Color grassColor = null;
    private Color foliageColor = null;

    private String music = null; //todo
    private String ambientSound = null; //todo
    private String additionsSound = null; //todo
    private String moodSound = null; //todo

    public BiomeInfo() {

    }

    public BiomeInfo(
            @NotNull String name, @NotNull Material displayItem,
            @NotNull Color skyColor, @NotNull Color fogColor,
            @NotNull Color waterColor, @NotNull Color waterFogColor,
            @NotNull Optional<Color> grassColor, @NotNull Optional<Color> foliageColor
    ) {
        this.name = name;
        this.displayItem = displayItem;
        this.skyColor = skyColor;
        this.fogColor = fogColor;
        this.waterColor = waterColor;
        this.waterFogColor = waterFogColor;
        this.grassColor = grassColor.orElse(null);
        this.foliageColor = foliageColor.orElse(null);
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @Nullable Key key() {
        if (getName().isEmpty()) return null;
        return Key.key("custom", getName());
    }

    public @NotNull Material getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(@NotNull Material displayItem) {
        this.displayItem = displayItem;
    }

    public @NotNull Color getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(@NotNull Color skyColor) {
        this.skyColor = skyColor;
    }

    public Color getFogColor() {
        return fogColor;
    }

    public void setFogColor(@NotNull Color fogColor) {
        this.fogColor = fogColor;
    }

    public @NotNull Color getWaterColor() {
        return waterColor;
    }

    public void setWaterColor(@NotNull Color waterColor) {
        this.waterColor = waterColor;
    }

    public @NotNull Color getWaterFogColor() {
        return waterFogColor;
    }

    public void setWaterFogColor(@NotNull Color waterFogColor) {
        this.waterFogColor = waterFogColor;
    }

    public @Nullable Color getGrassColor() {
        return grassColor;
    }

    public void setGrassColor(@Nullable Color grassColor) {
        this.grassColor = grassColor;
    }

    public @Nullable Color getFoliageColor() {
        return foliageColor;
    }

    public void setFoliageColor(@Nullable Color foliageColor) {
        this.foliageColor = foliageColor;
    }

    @ApiStatus.Internal
    public @Nullable Biome build() {
        var key = this.key();
        if (key == null) return null;

        var effects = BiomeEffects.builder()
                .skyColor(this.getSkyColor())
                .fogColor(this.getFogColor())
                .waterColor(this.getWaterColor())
                .waterFogColor(this.getWaterFogColor());
        if (this.getGrassColor() != null) effects.grassColor(this.getGrassColor());
        if (this.getFoliageColor() != null) effects.foliageColor(this.getFoliageColor());

        //TODO(1.21.5)
//        float temperature = switch (this.precipitation) {
//            case NONE -> 0.5f;
//            case RAIN -> 0.8f;
//            case SNOW -> 0.0f;
//        };

        return Biome.builder()
//                .temperature(temperature)
                .downfall(1.0F)
                .effects(effects.build())
                .build();
    }
}

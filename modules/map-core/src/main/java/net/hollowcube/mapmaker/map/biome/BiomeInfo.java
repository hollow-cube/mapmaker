package net.hollowcube.mapmaker.map.biome;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.color.Color;
import net.minestom.server.item.Material;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class BiomeInfo {

    private static final RGBLike DEFAULT_SKY_COLOR = new Color(0x78A7FF);
    private static final RGBLike DEFAULT_FOG_COLOR = new Color(0xC0D8FF);
    private static final RGBLike DEFAULT_WATER_COLOR = new Color(0x3F76E4);
    private static final RGBLike DEFAULT_WATER_FOG_COLOR = new Color(0x050533);

    public static final Codec<BiomeInfo> CODEC = StructCodec.struct(
        "name", Codec.STRING.optional(""), BiomeInfo::getName,
        "displayItem", Material.CODEC.optional(Material.GRASS_BLOCK), BiomeInfo::getDisplayItem,
        "skyColor", Color.CODEC.optional(DEFAULT_SKY_COLOR), BiomeInfo::getSkyColor,
        "fogColor", Color.CODEC.optional(DEFAULT_FOG_COLOR), BiomeInfo::getFogColor,
        "waterColor", Color.CODEC.optional(DEFAULT_WATER_COLOR), BiomeInfo::getWaterColor,
        "waterFogColor", Color.CODEC.optional(DEFAULT_WATER_FOG_COLOR), BiomeInfo::getWaterFogColor,
        "grassColor", Color.CODEC.optional(), BiomeInfo::getGrassColor,
        "foliageColor", Color.CODEC.optional(), BiomeInfo::getFoliageColor,
        BiomeInfo::new);

    public enum Precipitation {
        NONE,
        RAIN,
        SNOW
    }

    private String name = "";
    private Material displayItem = Material.GRASS_BLOCK;

    private Precipitation precipitation = Precipitation.NONE;
    private @Nullable Object particle = null; //todo

    private RGBLike skyColor = DEFAULT_SKY_COLOR;
    private RGBLike fogColor = DEFAULT_FOG_COLOR;
    private RGBLike waterColor = DEFAULT_WATER_COLOR;
    private RGBLike waterFogColor = DEFAULT_WATER_FOG_COLOR;
    private @Nullable RGBLike grassColor = null;
    private @Nullable RGBLike foliageColor = null;

    private @Nullable String music = null; //todo
    private @Nullable String ambientSound = null; //todo
    private @Nullable String additionsSound = null; //todo
    private @Nullable String moodSound = null; //todo

    public BiomeInfo() {

    }

    public BiomeInfo(
        String name, Material displayItem,
        RGBLike skyColor, RGBLike fogColor,
        RGBLike waterColor, RGBLike waterFogColor,
        @Nullable RGBLike grassColor, @Nullable RGBLike foliageColor
    ) {
        this.name = name;
        this.displayItem = displayItem;
        this.skyColor = skyColor;
        this.fogColor = fogColor;
        this.waterColor = waterColor;
        this.waterFogColor = waterFogColor;
        this.grassColor = grassColor;
        this.foliageColor = foliageColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable Key key() {
        if (getName().isEmpty()) return null;
        return Key.key("custom", getName().startsWith("custom:") ? getName().substring(7) : getName());
    }

    public Material getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(Material displayItem) {
        this.displayItem = displayItem;
    }

    public RGBLike getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(RGBLike skyColor) {
        this.skyColor = skyColor;
    }

    public RGBLike getFogColor() {
        return fogColor;
    }

    public void setFogColor(RGBLike fogColor) {
        this.fogColor = fogColor;
    }

    public RGBLike getWaterColor() {
        return waterColor;
    }

    public void setWaterColor(RGBLike waterColor) {
        this.waterColor = waterColor;
    }

    public RGBLike getWaterFogColor() {
        return waterFogColor;
    }

    public void setWaterFogColor(RGBLike waterFogColor) {
        this.waterFogColor = waterFogColor;
    }

    public @Nullable RGBLike getGrassColor() {
        return grassColor;
    }

    public void setGrassColor(@Nullable RGBLike grassColor) {
        this.grassColor = grassColor;
    }

    public @Nullable RGBLike getFoliageColor() {
        return foliageColor;
    }

    public void setFoliageColor(@Nullable RGBLike foliageColor) {
        this.foliageColor = foliageColor;
    }

    @ApiStatus.Internal
    public @Nullable Biome build() {
        var key = this.key();
        if (key == null) return null;

        var effects = BiomeEffects.builder()
            .waterColor(this.getWaterColor());
        if (this.getGrassColor() != null) effects.grassColor(this.getGrassColor());
        if (this.getFoliageColor() != null) effects.foliageColor(this.getFoliageColor());

        float temperature = switch (this.precipitation) {
            case NONE -> 0.5f;
            case RAIN -> 0.8f;
            case SNOW -> 0.0f;
        };

        return Biome.builder()
            .temperature(temperature)
            .downfall(1.0F)
            .effects(effects.build())
            .build();
    }
}

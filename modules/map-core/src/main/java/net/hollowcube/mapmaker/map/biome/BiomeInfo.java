package net.hollowcube.mapmaker.map.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class BiomeInfo {

    private static final String DEFAULT_SKY_COLOR = "#78A7FF";
    private static final String DEFAULT_FOG_COLOR = "#C0D8FF";
    private static final String DEFAULT_WATER_COLOR = "#3F76E4";
    private static final String DEFAULT_WATER_FOG_COLOR = "#050533";

    public static final Codec<BiomeInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(BiomeInfo::getName),
            ExtraCodecs.MATERIAL.optionalFieldOf("displayItem", Material.GRASS_BLOCK).forGetter(BiomeInfo::getDisplayItem),
            ExtraCodecs.EnumI(Biome.Precipitation.class).optionalFieldOf("precipitation", Biome.Precipitation.NONE).forGetter(BiomeInfo::getPrecipitation),
            Codec.STRING.optionalFieldOf("skyColor", DEFAULT_SKY_COLOR).forGetter(BiomeInfo::getSkyColor),
            Codec.STRING.optionalFieldOf("fogColor", DEFAULT_FOG_COLOR).forGetter(BiomeInfo::getFogColor),
            Codec.STRING.optionalFieldOf("waterColor", DEFAULT_WATER_COLOR).forGetter(BiomeInfo::getWaterColor),
            Codec.STRING.optionalFieldOf("waterFogColor", DEFAULT_WATER_FOG_COLOR).forGetter(BiomeInfo::getWaterFogColor),
            Codec.STRING.optionalFieldOf("grassColor").forGetter(BiomeInfo::getGrassColorSafe),
            Codec.STRING.optionalFieldOf("foliageColor").forGetter(BiomeInfo::getFoliageColorSafe)
    ).apply(i, BiomeInfo::new));

    private String name = "";
    private Material displayItem = Material.GRASS_BLOCK;

    private Biome.Precipitation precipitation = Biome.Precipitation.NONE;
    private Object particle = null; //todo

    private String skyColor = DEFAULT_SKY_COLOR;
    private String fogColor = DEFAULT_FOG_COLOR;
    private String waterColor = DEFAULT_WATER_COLOR;
    private String waterFogColor = DEFAULT_WATER_FOG_COLOR;
    private String grassColor = null;
    private String foliageColor = null;

    private String music = null; //todo
    private String ambientSound = null; //todo
    private String additionsSound = null; //todo
    private String moodSound = null; //todo

    public BiomeInfo() {

    }

    public BiomeInfo(
            @NotNull String name, @NotNull Material displayItem,
            @NotNull Biome.Precipitation precipitation,
            @NotNull String skyColor, @NotNull String fogColor,
            @NotNull String waterColor, @NotNull String waterFogColor,
            @NotNull Optional<String> grassColor, @NotNull Optional<String> foliageColor
    ) {
        this.name = name;
        this.displayItem = displayItem;
        this.precipitation = precipitation;
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

    public @Nullable NamespaceID namespace() {
        if (getName().isEmpty()) return null;
        return NamespaceID.from("custom", getName());
    }

    public @NotNull Material getDisplayItem() {
        return displayItem;
    }

    public void setDisplayItem(@NotNull Material displayItem) {
        this.displayItem = displayItem;
    }

    public @NotNull Biome.Precipitation getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(@NotNull Biome.Precipitation precipitation) {
        this.precipitation = precipitation;
    }

    public @NotNull String getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(@NotNull String skyColor) {
        Objects.requireNonNull(TextColor.fromCSSHexString(skyColor), "Invalid color: " + skyColor);
        this.skyColor = skyColor;
    }

    public String getFogColor() {
        return fogColor;
    }

    public void setFogColor(@NotNull String fogColor) {
        Objects.requireNonNull(TextColor.fromCSSHexString(fogColor), "Invalid color: " + fogColor);
        this.fogColor = fogColor;
    }

    public @NotNull String getWaterColor() {
        return waterColor;
    }

    public void setWaterColor(@NotNull String waterColor) {
        Objects.requireNonNull(TextColor.fromCSSHexString(waterColor), "Invalid color: " + waterColor);
        this.waterColor = waterColor;
    }

    public @NotNull String getWaterFogColor() {
        return waterFogColor;
    }

    public void setWaterFogColor(@NotNull String waterFogColor) {
        Objects.requireNonNull(TextColor.fromCSSHexString(waterFogColor), "Invalid color: " + waterFogColor);
        this.waterFogColor = waterFogColor;
    }

    public @Nullable String getGrassColor() {
        return grassColor;
    }

    public @NotNull Optional<String> getGrassColorSafe() {
        return Optional.ofNullable(grassColor);
    }

    public void setGrassColor(@Nullable String grassColor) {
        if (grassColor != null)
            Objects.requireNonNull(TextColor.fromCSSHexString(grassColor), "Invalid color: " + grassColor);
        this.grassColor = grassColor;
    }

    public @Nullable String getFoliageColor() {
        return foliageColor;
    }

    public @NotNull Optional<String> getFoliageColorSafe() {
        return Optional.ofNullable(foliageColor);
    }

    public void setFoliageColor(@Nullable String foliageColor) {
        if (foliageColor != null)
            Objects.requireNonNull(TextColor.fromCSSHexString(foliageColor), "Invalid color: " + foliageColor);
        this.foliageColor = foliageColor;
    }

}

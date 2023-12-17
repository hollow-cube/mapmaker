package net.hollowcube.mapmaker.hub.biome;

import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BiomeInfo {

    private String name = "";
    private Material displayItem = Material.GRASS_BLOCK;

    private Biome.Precipitation precipitation = Biome.Precipitation.NONE;
    private Object particle = null; //todo

    private String skyColor = "#78A7FF";
    private String fogColor = "#C0D8FF";
    private String waterColor = "#3F76E4";
    private String waterFogColor = "#050533";
    private String grassColor = null;
    private String foliageColor = null;

    private String music = null; //todo
    private String ambientSound = null; //todo
    private String additionsSound = null; //todo
    private String moodSound = null; //todo

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
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

    public void setGrassColor(@Nullable String grassColor) {
        if (grassColor != null)
            Objects.requireNonNull(TextColor.fromCSSHexString(grassColor), "Invalid color: " + grassColor);
        this.grassColor = grassColor;
    }

    public @Nullable String getFoliageColor() {
        return foliageColor;
    }

    public void setFoliageColor(@Nullable String foliageColor) {
        if (foliageColor != null)
            Objects.requireNonNull(TextColor.fromCSSHexString(foliageColor), "Invalid color: " + foliageColor);
        this.foliageColor = foliageColor;
    }

}

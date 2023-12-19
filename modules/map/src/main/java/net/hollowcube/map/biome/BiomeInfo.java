package net.hollowcube.map.biome;

import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
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

    private transient Biome minestomBiome = null;

    public BiomeInfo() {

    }

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

    // Minestom biome association

    public boolean isLoaded() {
        return minestomBiome != null;
    }

    public @UnknownNullability Biome getMinestomBiome() {
        return minestomBiome;
    }

    public void setMinestomBiome(@Nullable Biome minestomBiome) {
        this.minestomBiome = minestomBiome;
    }

    // Serialization

    public BiomeInfo(@NotNull BiomeProtos.BiomeInfo bi) {
        this.name = bi.getName();
        this.displayItem = Material.fromNamespaceId(bi.getDisplayItem());

        this.precipitation = Biome.Precipitation.values()[bi.getPrecipitation()];

        this.skyColor = bi.getSkyColor();
        this.fogColor = bi.getFogColor();
        this.waterColor = bi.getWaterColor();
        this.waterFogColor = bi.getWaterFogColor();
        if (bi.hasGrassColor()) this.grassColor = bi.getGrassColor();
        if (bi.hasFoliageColor()) this.foliageColor = bi.getFoliageColor();
    }

    public @NotNull BiomeProtos.BiomeInfo toProto() {
        var builder = BiomeProtos.BiomeInfo.newBuilder()
                .setName(name)
                .setDisplayItem(displayItem.name())
                .setPrecipitation(precipitation.ordinal())
                .setSkyColor(skyColor)
                .setFogColor(fogColor)
                .setWaterColor(waterColor)
                .setWaterFogColor(waterFogColor);
        if (grassColor != null) builder = builder.setGrassColor(grassColor);
        if (foliageColor != null) builder = builder.setFoliageColor(foliageColor);
        return builder.build();
    }

}

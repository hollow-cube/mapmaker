package net.hollowcube.map.biome;

import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biomes.Biome;
import net.minestom.server.world.biomes.BiomeEffects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.minestom.server.network.NetworkBuffer.BOOLEAN;
import static net.minestom.server.network.NetworkBuffer.STRING;

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

    public BiomeInfo(@NotNull NetworkBuffer buffer) {
        this.name = buffer.read(STRING);
        this.displayItem = Material.fromNamespaceId(buffer.read(STRING));
        this.precipitation = buffer.readEnum(Biome.Precipitation.class);
        buffer.read(BOOLEAN); //todo particle (for now reads never present optional)
        this.skyColor = buffer.read(STRING);
        this.fogColor = buffer.read(STRING);
        this.waterColor = buffer.read(STRING);
        this.waterFogColor = buffer.read(STRING);
        this.grassColor = buffer.readOptional(STRING);
        this.foliageColor = buffer.readOptional(STRING);
        buffer.read(BOOLEAN); //todo music (for now reads never present optional)
        buffer.read(BOOLEAN); //todo ambientSound (for now reads never present optional)
        buffer.read(BOOLEAN); //todo additionsSound (for now reads never present optional)
        buffer.read(BOOLEAN); //todo moodSound (for now reads never present optional)
    }

    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(STRING, name);
        buffer.write(STRING, displayItem.name());
        buffer.writeEnum(Biome.Precipitation.class, precipitation);
        buffer.write(BOOLEAN, false); //todo particle (for now writes never present optional)
        buffer.write(STRING, skyColor);
        buffer.write(STRING, fogColor);
        buffer.write(STRING, waterColor);
        buffer.write(STRING, waterFogColor);
        buffer.writeOptional(STRING, grassColor);
        buffer.writeOptional(STRING, foliageColor);
        buffer.write(BOOLEAN, false); //todo music (for now writes never present optional)
        buffer.write(BOOLEAN, false); //todo ambientSound (for now writes never present optional)
        buffer.write(BOOLEAN, false); //todo additionsSound (for now writes never present optional)
        buffer.write(BOOLEAN, false); //todo moodSound (for now writes never present optional)
    }

    /**
     * Converts this biome info into a Minestom biome.
     *
     * <p><b>WARNING:</b> this will create a new Minestom biome with a new ID. It should most likely only be called
     * by {@link BiomeContainer}, and you should use that to get the instance of the biome.</p>
     *
     * @return The biome, or null if it is incomplete (i.e missing a name)
     */
    public @Nullable Biome toMinestomBiome() {
        if (name.isEmpty()) return null;

        var effectBuilder = BiomeEffects.builder()
                .skyColor(TextColor.fromCSSHexString(skyColor).value())
                .fogColor(TextColor.fromCSSHexString(fogColor).value())
                .waterColor(TextColor.fromCSSHexString(waterColor).value())
                .waterFogColor(TextColor.fromCSSHexString(waterFogColor).value());
        if (grassColor != null) effectBuilder.grassColor(TextColor.fromCSSHexString(grassColor).value());
        if (foliageColor != null) effectBuilder.foliageColor(TextColor.fromCSSHexString(foliageColor).value());

        return Biome.builder()
                .name(NamespaceID.from(name))
                .category(Biome.Category.NONE)
                .temperature(0.8F) // IDK what this affects
                .downfall(0.4F) // IDK what this affects
                .depth(0.125F) // IDK what this affects
                .scale(0.05F) // IDK what this affects
                .effects(effectBuilder.build())
                .build();
    }

}

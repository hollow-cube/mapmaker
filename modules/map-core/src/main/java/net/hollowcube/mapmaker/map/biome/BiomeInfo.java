package net.hollowcube.mapmaker.map.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.biome.Biome;
import net.minestom.server.world.biome.BiomeEffects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BiomeInfo {

    private static final TextColor DEFAULT_SKY_COLOR = TextColor.color(0x78A7FF);
    private static final TextColor DEFAULT_FOG_COLOR = TextColor.color(0xC0D8FF);
    private static final TextColor DEFAULT_WATER_COLOR = TextColor.color(0x3F76E4);
    private static final TextColor DEFAULT_WATER_FOG_COLOR = TextColor.color(0x050533);

    public static final Codec<BiomeInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(BiomeInfo::getName),
            ExtraCodecs.MATERIAL.lenientOptionalFieldOf("displayItem", Material.GRASS_BLOCK).forGetter(BiomeInfo::getDisplayItem),
            ExtraCodecs.Enum(Biome.Precipitation.class).lenientOptionalFieldOf("precipitation", Biome.Precipitation.NONE).forGetter(BiomeInfo::getPrecipitation),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("skyColor", DEFAULT_SKY_COLOR).forGetter(BiomeInfo::getSkyColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("fogColor", DEFAULT_FOG_COLOR).forGetter(BiomeInfo::getFogColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("waterColor", DEFAULT_WATER_COLOR).forGetter(BiomeInfo::getWaterColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("waterFogColor", DEFAULT_WATER_FOG_COLOR).forGetter(BiomeInfo::getWaterFogColor),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("grassColor").forGetter(ExtraCodecs.optional(BiomeInfo::getGrassColor)),
            ExtraCodecs.COLOR.lenientOptionalFieldOf("foliageColor").forGetter(ExtraCodecs.optional(BiomeInfo::getFoliageColor))
    ).apply(i, BiomeInfo::new));

    private String name = "";
    private Material displayItem = Material.GRASS_BLOCK;

    private Biome.Precipitation precipitation = Biome.Precipitation.NONE;
    private Object particle = null; //todo

    private TextColor skyColor = DEFAULT_SKY_COLOR;
    private TextColor fogColor = DEFAULT_FOG_COLOR;
    private TextColor waterColor = DEFAULT_WATER_COLOR;
    private TextColor waterFogColor = DEFAULT_WATER_FOG_COLOR;
    private TextColor grassColor = null;
    private TextColor foliageColor = null;

    private String music = null; //todo
    private String ambientSound = null; //todo
    private String additionsSound = null; //todo
    private String moodSound = null; //todo

    public BiomeInfo() {

    }

    public BiomeInfo(
            @NotNull String name, @NotNull Material displayItem,
            @NotNull Biome.Precipitation precipitation,
            @NotNull TextColor skyColor, @NotNull TextColor fogColor,
            @NotNull TextColor waterColor, @NotNull TextColor waterFogColor,
            @NotNull Optional<TextColor> grassColor, @NotNull Optional<TextColor> foliageColor
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

    public @NotNull TextColor getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(@NotNull TextColor skyColor) {
        this.skyColor = skyColor;
    }

    public TextColor getFogColor() {
        return fogColor;
    }

    public void setFogColor(@NotNull TextColor fogColor) {
        this.fogColor = fogColor;
    }

    public @NotNull TextColor getWaterColor() {
        return waterColor;
    }

    public void setWaterColor(@NotNull TextColor waterColor) {
        this.waterColor = waterColor;
    }

    public @NotNull TextColor getWaterFogColor() {
        return waterFogColor;
    }

    public void setWaterFogColor(@NotNull TextColor waterFogColor) {
        this.waterFogColor = waterFogColor;
    }

    public @Nullable TextColor getGrassColor() {
        return grassColor;
    }

    public void setGrassColor(@Nullable TextColor grassColor) {
        this.grassColor = grassColor;
    }

    public @Nullable TextColor getFoliageColor() {
        return foliageColor;
    }

    public void setFoliageColor(@Nullable TextColor foliageColor) {
        this.foliageColor = foliageColor;
    }

    @ApiStatus.Internal
    public @Nullable Biome build() {
        var namespace = this.namespace();
        if (namespace == null) return null;

        var effects = BiomeEffects.builder()
                .skyColor(this.getSkyColor().value())
                .fogColor(this.getFogColor().value())
                .waterColor(this.getWaterColor().value())
                .waterFogColor(this.getWaterFogColor().value());
        if (this.getGrassColor() != null) effects.grassColor(this.getGrassColor().value());
        if (this.getFoliageColor() != null) effects.foliageColor(this.getFoliageColor().value());

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

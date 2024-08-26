package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;

public record BoneConfig(
        @NotNull AbstractDisplayMeta.BillboardConstraints billboard,
        boolean overrideBrightness,
        int brightnessOverride,
        boolean enchanted,
        boolean glowing,
        boolean overrideGlowColor,
        @NotNull String glowColor,
        boolean inheritSettings,
        boolean invisible,
        boolean useNbt,
        @NotNull String nbt,
        int shadowRadius,
        int shadowStrength
) {
    public static final Codec<BoneConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.enumString(AbstractDisplayMeta.BillboardConstraints.class)
                    .optionalFieldOf("billboard", AbstractDisplayMeta.BillboardConstraints.FIXED).forGetter(BoneConfig::billboard),
            Codec.BOOL.optionalFieldOf("override_brightness", false).forGetter(BoneConfig::overrideBrightness),
            Codec.INT.optionalFieldOf("brightness_override", 1).forGetter(BoneConfig::brightnessOverride),
            Codec.BOOL.optionalFieldOf("enchanted", false).forGetter(BoneConfig::enchanted),
            Codec.BOOL.optionalFieldOf("glowing", false).forGetter(BoneConfig::glowing),
            Codec.BOOL.optionalFieldOf("override_glow_color", false).forGetter(BoneConfig::overrideGlowColor),
            Codec.STRING.optionalFieldOf("glow_color", "#ffffff").forGetter(BoneConfig::glowColor),
            Codec.BOOL.optionalFieldOf("inherit_settings", true).forGetter(BoneConfig::inheritSettings),
            Codec.BOOL.optionalFieldOf("invisible", false).forGetter(BoneConfig::invisible),
            Codec.BOOL.optionalFieldOf("use_nbt", false).forGetter(BoneConfig::useNbt),
            Codec.STRING.optionalFieldOf("nbt", "{}").forGetter(BoneConfig::nbt),
            Codec.INT.optionalFieldOf("shadow_radius", 0).forGetter(BoneConfig::shadowRadius),
            Codec.INT.optionalFieldOf("shadow_strength", 0).forGetter(BoneConfig::shadowStrength)
    ).apply(i, BoneConfig::new));
}

package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.util.ExtraCodecs;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record ModelSettings(
        @NotNull String exportNamespace,
        //TODO: bounding_box
        @NotNull Material displayItem,
        int customModelDataOffset,
        boolean bakedAnimations
) {
    private static final Function<Boolean, DataResult<Boolean>> NEVER_BAKED = b ->
            b ? DataResult.error("Baked animations are not supported") : DataResult.success(false);

    public static final Codec<ModelSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("export_namespace").forGetter(ModelSettings::exportNamespace),
            // TODO: bounding_box
            ExtraCodecs.MATERIAL.optionalFieldOf("display_item", Material.WHITE_DYE).forGetter(ModelSettings::displayItem),
            Codec.INT.fieldOf("custom_model_data_offset").forGetter(ModelSettings::customModelDataOffset),
            Codec.BOOL.flatXmap(NEVER_BAKED, NEVER_BAKED).fieldOf("baked_animations").forGetter(ModelSettings::bakedAnimations)
    ).apply(i, ModelSettings::new));
}

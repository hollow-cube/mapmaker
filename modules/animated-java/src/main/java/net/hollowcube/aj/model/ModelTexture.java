package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

public record ModelTexture(
        @NotNull String name,
        @NotNull String src
) {
    public static final Codec<ModelTexture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ModelTexture::name),
            Codec.STRING.fieldOf("src").forGetter(ModelTexture::src)
    ).apply(i, ModelTexture::new));
}

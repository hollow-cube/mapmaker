package net.hollowcube.aj;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public record Texture(
        @NotNull String name,
        @NotNull String src
) {
    public static final StructCodec<Texture> CODEC = StructCodec.struct(
            "name", Codec.STRING, Texture::name,
            "src", Codec.STRING, Texture::src,
            Texture::new);
}

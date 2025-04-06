package net.hollowcube.mapmaker.player;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PlayerSkin(
        @Nullable String texture,
        @Nullable String signature
) {
    public static final StructCodec<PlayerSkin> CODEC = StructCodec.struct(
            "texture", Codec.STRING.optional(), PlayerSkin::texture,
            "signature", Codec.STRING.optional(), PlayerSkin::signature,
            PlayerSkin::new);

    public PlayerSkin(@NotNull net.minestom.server.entity.PlayerSkin skin) {
        this(skin.textures(), skin.signature());
    }

    public @NotNull net.minestom.server.entity.PlayerSkin into() {
        return new net.minestom.server.entity.PlayerSkin(texture, signature);
    }

    @Override
    public String toString() {
        return String.format("PlayerSkin{texture=%b, signature=%b}", texture, signature);
    }
}

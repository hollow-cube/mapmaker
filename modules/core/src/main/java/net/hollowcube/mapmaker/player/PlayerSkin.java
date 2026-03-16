package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record PlayerSkin(
    @Nullable String texture,
    @Nullable String signature
) {
    public static final StructCodec<PlayerSkin> CODEC = StructCodec.struct(
            "texture", Codec.STRING.optional(), PlayerSkin::texture,
            "signature", Codec.STRING.optional(), PlayerSkin::signature,
            PlayerSkin::new);

    public PlayerSkin(net.minestom.server.entity.PlayerSkin skin) {
        this(skin.textures(), skin.signature());
    }

    public net.minestom.server.entity.PlayerSkin into() {
        return new net.minestom.server.entity.PlayerSkin(texture, signature);
    }

    @Override
    public String toString() {
        return String.format("PlayerSkin{texture=%b, signature=%b}", texture, signature);
    }
}

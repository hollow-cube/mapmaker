package net.hollowcube.mapmaker.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PlayerSkin(
        @Nullable String texture,
        @Nullable String signature
) {

    public static final Codec<PlayerSkin> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("texture").forGetter(PlayerSkin::optTexture),
            Codec.STRING.optionalFieldOf("signature").forGetter(PlayerSkin::optSignature)
    ).apply(i, PlayerSkin::new));

    public PlayerSkin(@NotNull Optional<String> texture, @NotNull Optional<String> signature) {
        this(texture.orElse(null), signature.orElse(null));
    }

    public PlayerSkin(@NotNull net.minestom.server.entity.PlayerSkin skin) {
        this(skin.textures(), skin.signature());
    }

    public @NotNull Optional<String> optTexture() {
        return Optional.ofNullable(texture);
    }

    public @NotNull Optional<String> optSignature() {
        return Optional.ofNullable(signature);
    }

    public @NotNull net.minestom.server.entity.PlayerSkin into() {
        return new net.minestom.server.entity.PlayerSkin(texture, signature);
    }

    @Override
    public String toString() {
        return String.format("PlayerSkin{texture=%b, signature=%b}", texture, signature);
    }
}

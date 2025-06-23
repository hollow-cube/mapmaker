package net.hollowcube.aj;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record Model(
        @NotNull Settings settings,
        @NotNull Map<UUID, Texture> textures,
        @NotNull Map<UUID, Node> nodes,
        @NotNull Map<UUID, Variant> variants,
        @NotNull Map<UUID, Animation> animations
) {
    public static final StructCodec<Model> CODEC = StructCodec.struct(
            "settings", Settings.CODEC, Model::settings,
            "textures", Codec.UUID_COERCED.mapValue(Texture.CODEC), Model::textures,
            "nodes", Codec.UUID_COERCED.mapValue(Node.CODEC), Model::nodes,
            "variants", Codec.UUID_COERCED.mapValue(Variant.CODEC), Model::variants,
            "animations", Codec.UUID_COERCED.mapValue(Animation.CODEC), Model::animations,
            Model::new);

    public record Settings(
            @NotNull String exportNamespace
    ) {
        public static final StructCodec<Settings> CODEC = StructCodec.struct(
                "export_namespace", Codec.STRING, Settings::exportNamespace,
                Settings::new);
    }
}

package net.hollowcube.aj;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record Model(
        @NotNull Map<UUID, Node> nodes,
        @NotNull Map<UUID, Animation> animations
) {
    public static final StructCodec<Model> CODEC = StructCodec.struct(
            "nodes", Codec.UUID_COERCED.mapValue(Node.CODEC), Model::nodes,
            "animations", Codec.UUID_COERCED.mapValue(Animation.CODEC), Model::animations,
            Model::new);
}

package net.hollowcube.aj;

import net.hollowcube.aj.util.AJCodecUtil;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public record Transform(
        @NotNull Vec translation,
        float @NotNull [] leftRotation,
        @NotNull Vec scale
) {
    public static final StructCodec<Transform> CODEC = StructCodec.struct(
            "translation", AJCodecUtil.VEC, Transform::translation,
            "left_rotation", AJCodecUtil.QUATERNION, Transform::leftRotation,
            "scale", AJCodecUtil.VEC, Transform::scale,
            Transform::new);

    public record Default(
            @NotNull Transform decomposed,
            float @NotNull [] headRotation
    ) {
        public static final StructCodec<Default> CODEC = StructCodec.struct(
                "decomposed", Transform.CODEC, Default::decomposed,
                "head_rot", AJCodecUtil.FLOAT2, Default::headRotation,
                Default::new);
    }
}

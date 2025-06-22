package net.hollowcube.aj;

import net.hollowcube.aj.util.AJCodecUtil;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record Animation(
        @NotNull String name,
        @NotNull LoopMode loopMode,
        float duration, // in seconds
        @NotNull List<UUID> excludedNodes,
        @NotNull Map<UUID, List<Keyframe>> animators
) {
    public static final StructCodec<Animation> CODEC = StructCodec.struct(
            "name", Codec.STRING, Animation::name,
            "loop_mode", LoopMode.CODEC, Animation::loopMode,
            "duration", Codec.FLOAT, Animation::duration,
            "excluded_nodes", Codec.UUID_COERCED.list().optional(List.of()), Animation::excludedNodes,
            "animators", Codec.UUID_COERCED.mapValue(Keyframe.CODEC.list()), Animation::animators,
            Animation::new);

    public enum LoopMode {
        HOLD,
        ; // todo other values

        public static final Codec<LoopMode> CODEC = Codec.Enum(LoopMode.class);
    }

    public enum Channel {
        POSITION,
        ROTATION,
        SCALE;

        public static final Codec<Channel> CODEC = Codec.Enum(Channel.class);
    }

    public record Keyframe(
            float time, // in seconds
            @NotNull Channel channel,
            @NotNull Vec value // todo support molang
            // todo interpolation
    ) {
        public static final StructCodec<Keyframe> CODEC = StructCodec.struct(
                "time", Codec.FLOAT, Keyframe::time,
                "channel", Channel.CODEC, Keyframe::channel,
                "value", AJCodecUtil.VEC_COERCED, Keyframe::value,
                Keyframe::new);
    }
}

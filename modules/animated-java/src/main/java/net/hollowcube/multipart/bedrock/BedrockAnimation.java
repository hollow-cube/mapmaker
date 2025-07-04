package net.hollowcube.multipart.bedrock;

import net.hollowcube.aj.animation.LoopMode;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public record BedrockAnimation(
        @NotNull String formatVersion,
        @NotNull Map<String, Animation> animations
) {
    public static final StructCodec<BedrockAnimation> CODEC = StructCodec.struct(
            "format_version", Codec.STRING, BedrockAnimation::formatVersion,
            "animations", Codec.STRING.mapValue(Animation.CODEC), BedrockAnimation::animations,
            BedrockAnimation::new);

    public record Animation(
            // We don't support 'anim_time_update', 'blend_weight', 'start_delay', 'loop_delay', or 'override_previous_animation' yet.
            @NotNull LoopMode loop,
            double animationLength,
            @NotNull Map<String, Animator> bones
    ) {
        private static final Codec<LoopMode> LOOP_CODEC = new Codec<>() {
            @Override
            public @NotNull <D> Result<LoopMode> decode(@NotNull Transcoder<D> coder, @NotNull D value) {
                var stringResult = coder.getString(value);
                if (stringResult instanceof Result.Ok(var string)) {
                    return switch (string) {
                        case "hold_on_last_frame" -> new Result.Ok<>(LoopMode.HOLD);
                        case "true" -> new Result.Ok<>(LoopMode.ONCE);
                        case "false" -> new Result.Ok<>(LoopMode.LOOP);
                        default -> new Result.Error<>("Invalid loop mode: " + string);
                    };
                } else if (coder.getBoolean(value) instanceof Result.Ok(var bool)) {
                    return new Result.Ok<>(bool ? LoopMode.ONCE : LoopMode.LOOP);
                } else return new Result.Error<>("Invalid loop mode value: " + value);
            }

            @Override
            public @NotNull <D> Result<D> encode(@NotNull Transcoder<D> coder, @Nullable LoopMode value) {
                if (value == null) return new Result.Error<>("null");
                return new Result.Ok<>(switch (value) {
                    case ONCE -> coder.createBoolean(true);
                    case LOOP -> coder.createBoolean(false);
                    case HOLD -> coder.createString("hold_on_last_frame");
                });
            }
        };
        public static final StructCodec<Animation> CODEC = StructCodec.struct(
                "loop", LOOP_CODEC.optional(LoopMode.ONCE), Animation::loop,
                "animation_length", Codec.DOUBLE, Animation::animationLength,
                "bones", Codec.STRING.mapValue(Animator.CODEC), Animation::bones,
                Animation::new);

    }

    public record Animator(
            // We don't support 'relative_to' here yet.
            @NotNull List<Keyframe> position,
            @NotNull List<Keyframe> rotation,
            @NotNull List<Keyframe> scale
    ) {
        public static final StructCodec<Animator> CODEC = StructCodec.struct(
                "position", Keyframe.LIST_CODEC.optional(List.of()), Animator::position,
                "rotation", Keyframe.LIST_CODEC.optional(List.of()), Animator::rotation,
                "scale", Keyframe.LIST_CODEC.optional(List.of()), Animator::scale,
                Animator::new);
    }

    public record Keyframe(
            double time,
            @NotNull Vec value
    ) {
        public static final Codec<List<Keyframe>> LIST_CODEC = new Codec<>() {
            @Override
            public @NotNull <D> Result<List<Keyframe>> decode(@NotNull Transcoder<D> coder, @NotNull D value) {
                var doubleResult = coder.getDouble(value);
                if (doubleResult instanceof Result.Ok(var doubleValue))
                    return new Result.Ok<>(List.of(new Keyframe(0, new Vec(doubleValue))));
                var listResult = coder.getList(value);
                if (listResult instanceof Result.Ok(var list)) {
                    if (list.size() != 3) return new Result.Error<>("Expected 3 values, got: " + list.size());

                    var xResult = coder.getDouble(list.get(0));
                    if (!(xResult instanceof Result.Ok(var x)))
                        return xResult.cast();
                    var yResult = coder.getDouble(list.get(1));
                    if (!(yResult instanceof Result.Ok(var y)))
                        return yResult.cast();
                    var zResult = coder.getDouble(list.get(2));
                    if (!(zResult instanceof Result.Ok(var z)))
                        return zResult.cast();
                    return new Result.Ok<>(List.of(new Keyframe(0, new Vec(x, y, z))));
                }
                var mapResult = coder.getMap(value);
                if (!(mapResult instanceof Result.Ok(var map)))
                    return mapResult.cast();

                var keyframes = new ArrayList<Keyframe>(map.size());
                for (var key : map.keys()) {
                    var valueResult = map.getValue(key);
                    if (!(valueResult instanceof Result.Ok(var entry)))
                        return valueResult.cast();
                    var entryResult = coder.getList(entry);
                    if (!(entryResult instanceof Result.Ok(var list)))
                        return entryResult.cast();
                    if (list.size() != 3) return new Result.Error<>("Expected 3 values, got: " + list.size());

                    var xResult = coder.getDouble(list.get(0));
                    if (!(xResult instanceof Result.Ok(var x)))
                        return xResult.cast();
                    var yResult = coder.getDouble(list.get(1));
                    if (!(yResult instanceof Result.Ok(var y)))
                        return yResult.cast();
                    var zResult = coder.getDouble(list.get(2));
                    if (!(zResult instanceof Result.Ok(var z)))
                        return zResult.cast();

                    keyframes.add(new Keyframe(Double.parseDouble(key), new Vec(x, y, z)));
                }
                keyframes.sort(Comparator.comparingDouble(Keyframe::time));
                return new Result.Ok<>(List.copyOf(keyframes));
            }

            @Override
            public @NotNull <D> Result<D> encode(@NotNull Transcoder<D> coder, @Nullable List<Keyframe> value) {
                if (value == null || value.isEmpty()) return new Result.Error<>("null");
                if (value.size() == 1) {
                    var first = value.getFirst();
                    // If its at 0 and all components are the same we can just serialize a single value.
                    if (first.time == 0 && first.value.x() == first.value.y() && first.value.y() == first.value.z())
                        return new Result.Ok<>(coder.createDouble(first.value.x()));

                    var vec = coder.createList(3);
                    vec.add(coder.createDouble(first.value.x()));
                    vec.add(coder.createDouble(first.value.y()));
                    vec.add(coder.createDouble(first.value.z()));
                    return new Result.Ok<>(vec.build());
                }

                // Note that this does not preserve order, we don't support that in transcoder maps currently.
                var result = coder.createMap();
                for (var keyframe : value) {
                    var key = coder.createString(String.valueOf(keyframe.time));
                    var vec = coder.createList(3);
                    vec.add(coder.createDouble(keyframe.value.x()));
                    vec.add(coder.createDouble(keyframe.value.y()));
                    vec.add(coder.createDouble(keyframe.value.z()));
                    result.put(key, vec.build());
                }
                return new Result.Ok<>(result.build());
            }
        };

    }
}

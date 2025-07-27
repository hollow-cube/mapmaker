package net.hollowcube.common.util;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public record RelativePos(
        @NotNull Pos inner,
        int relativeFlags
) {
    public static final RelativePos ABS_ZERO = new RelativePos(Pos.ZERO, 0);
    public static final RelativePos REL_ZERO = new RelativePos(Pos.ZERO, RelativeFlags.COORD | RelativeFlags.VIEW);

    public static final StructCodec<RelativePos> CODEC = new CodecImpl();
    public static final Codec<RelativePos> ARRAY_CODEC = new ArrayCodecImpl();

    public @NotNull String strX() {
        return (relativeFlags & RelativeFlags.X) != 0 ? "~" + inner.x() : String.valueOf(inner.x());
    }

    public @NotNull String strY() {
        return (relativeFlags & RelativeFlags.Y) != 0 ? "~" + inner.y() : String.valueOf(inner.y());
    }

    public @NotNull String strZ() {
        return (relativeFlags & RelativeFlags.Z) != 0 ? "~" + inner.z() : String.valueOf(inner.z());
    }

    public @NotNull String strYaw() {
        return (relativeFlags & RelativeFlags.YAW) != 0 ? "~" + inner.yaw() : String.valueOf(inner.yaw());
    }

    public @NotNull String strPitch() {
        return (relativeFlags & RelativeFlags.PITCH) != 0 ? "~" + inner.pitch() : String.valueOf(inner.pitch());
    }

    public @NotNull RelativePos withStrX(@NotNull String x) {
        int newFlags = relativeFlags;
        if (x.startsWith("~")) {
            newFlags |= RelativeFlags.X;
            x = x.substring(1);
        } else newFlags &= ~RelativeFlags.X;
        return new RelativePos(inner.withX(Double.parseDouble(x)), newFlags);
    }

    public @NotNull RelativePos withStrY(@NotNull String y) {
        int newFlags = relativeFlags;
        if (y.startsWith("~")) {
            newFlags |= RelativeFlags.Y;
            y = y.substring(1);
        } else newFlags &= ~RelativeFlags.Y;
        return new RelativePos(inner.withY(Double.parseDouble(y)), newFlags);
    }

    public @NotNull RelativePos withStrZ(@NotNull String z) {
        int newFlags = relativeFlags;
        if (z.startsWith("~")) {
            newFlags |= RelativeFlags.Z;
            z = z.substring(1);
        } else newFlags &= ~RelativeFlags.Z;
        return new RelativePos(inner.withZ(Double.parseDouble(z)), newFlags);
    }

    public @NotNull RelativePos withStrYaw(@NotNull String yaw) {
        int newFlags = relativeFlags;
        if (yaw.startsWith("~")) {
            newFlags |= RelativeFlags.YAW;
            yaw = yaw.substring(1);
        } else newFlags &= ~RelativeFlags.YAW;
        float yawFloat = Float.parseFloat(yaw);
        return new RelativePos(inner.withYaw((yawFloat + 180) % 360 - 180), newFlags);
    }

    public @NotNull RelativePos withStrPitch(@NotNull String pitch) {
        int newFlags = relativeFlags;
        if (pitch.startsWith("~")) {
            newFlags |= RelativeFlags.PITCH;
            pitch = pitch.substring(1);
        } else newFlags &= ~RelativeFlags.PITCH;
        float pitchFloat = Float.parseFloat(pitch);
        return new RelativePos(inner.withPitch((pitchFloat + 180) % 360 - 180), newFlags);
    }

    public @NotNull Pos resolve(@NotNull Pos relative) {
        double resultX = inner.x(), resultY = inner.y(), resultZ = inner.z();
        if ((relativeFlags & RelativeFlags.X) != 0) resultX += relative.x();
        if ((relativeFlags & RelativeFlags.Y) != 0) resultY += relative.y();
        if ((relativeFlags & RelativeFlags.Z) != 0) resultZ += relative.z();

        float yaw = inner.yaw(), pitch = inner.pitch();
        if ((relativeFlags & RelativeFlags.YAW) != 0) yaw += relative.yaw();
        if ((relativeFlags & RelativeFlags.PITCH) != 0) pitch += relative.pitch();

        return new Pos(resultX, resultY, resultZ, yaw, pitch);
    }

    private static class CodecImpl implements StructCodec<RelativePos> {

        @Override
        public @NotNull <D> Result<RelativePos> decodeFromMap(@NotNull Transcoder<D> coder, Transcoder.@NotNull MapLike<D> map) {
            var xResult = parseNumberOrString(coder, map, "x");
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = parseNumberOrString(coder, map, "y");
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = parseNumberOrString(coder, map, "z");
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();
            var yawResult = parseNumberOrString(coder, map, "yaw");
            if (!(yawResult instanceof Result.Ok(var yaw))) return yawResult.cast();
            var pitchResult = parseNumberOrString(coder, map, "pitch");
            if (!(pitchResult instanceof Result.Ok(var pitch))) return pitchResult.cast();

            int flags = 0;
            if (x.isRight()) flags |= RelativeFlags.X;
            if (y.isRight()) flags |= RelativeFlags.Y;
            if (z.isRight()) flags |= RelativeFlags.Z;
            if (yaw.isRight()) flags |= RelativeFlags.YAW;
            if (yaw.isRight()) flags |= RelativeFlags.PITCH;
            return new Result.Ok<>(new RelativePos(new Pos(
                    x.map(Function.identity(), Function.identity()),
                    y.map(Function.identity(), Function.identity()),
                    z.map(Function.identity(), Function.identity()),
                    yaw.map(Function.identity(), Function.identity()).floatValue(),
                    pitch.map(Function.identity(), Function.identity()).floatValue()
            ), flags));
        }

        @Override
        public @NotNull <D> Result<D> encodeToMap(@NotNull Transcoder<D> coder, @NotNull RelativePos value, Transcoder.@NotNull MapBuilder<D> map) {
            map.put("x", (value.relativeFlags & RelativeFlags.X) != 0
                    ? coder.createString("~" + value.inner.x())
                    : coder.createDouble(value.inner.x()));
            map.put("y", (value.relativeFlags & RelativeFlags.Y) != 0
                    ? coder.createString("~" + value.inner.y())
                    : coder.createDouble(value.inner.y()));
            map.put("z", (value.relativeFlags & RelativeFlags.Z) != 0
                    ? coder.createString("~" + value.inner.z())
                    : coder.createDouble(value.inner.z()));
            map.put("yaw", (value.relativeFlags & RelativeFlags.YAW) != 0
                    ? coder.createString("~" + value.inner.yaw())
                    : coder.createFloat(value.inner.yaw()));
            map.put("pitch", (value.relativeFlags & RelativeFlags.PITCH) != 0
                    ? coder.createString("~" + value.inner.pitch())
                    : coder.createFloat(value.inner.pitch()));
            return new Result.Ok<>(map.build());
        }

        // Right either means its relative
        private <D> Result<Either<Double, Double>> parseNumberOrString(@NotNull Transcoder<D> coder, Transcoder.@NotNull MapLike<D> map, String key) {
            if (!map.hasValue(key)) return new Result.Ok<>(Either.right(0.0));
            var result = map.getValue(key);
            if (!(result instanceof Result.Ok(D raw))) return result.cast();
            if (coder.getDouble(raw) instanceof Result.Ok(Double number))
                return new Result.Ok<>(Either.left(number));
            try {
                if (coder.getString(raw) instanceof Result.Ok(String str)) {
                    if (str.startsWith("~"))
                        return new Result.Ok<>(Either.right(Double.parseDouble(str.substring(1))));
                    return new Result.Ok<>(Either.left(Double.parseDouble(str)));
                }

                return new Result.Error<>("Coordinate must be number or string, but got " + result);
            } catch (NumberFormatException ignored) {
                // Just default to 0.0
                return new Result.Ok<>(Either.right(0.0));
            }
        }
    }

    private static class ArrayCodecImpl implements Codec<RelativePos> {
        @Override
        public @NotNull <D> Result<RelativePos> decode(@NotNull Transcoder<D> coder, @NotNull D value) {
            var listResult = coder.getList(value);
            if (!(listResult instanceof Result.Ok(var list)))
                return listResult.cast();

            var xResult = parseNumberOrString(coder, list, 0);
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = parseNumberOrString(coder, list, 1);
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = parseNumberOrString(coder, list, 2);
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();
            var yawResult = parseNumberOrString(coder, list, 3);
            if (!(yawResult instanceof Result.Ok(var yaw))) return yawResult.cast();
            var pitchResult = parseNumberOrString(coder, list, 4);
            if (!(pitchResult instanceof Result.Ok(var pitch))) return pitchResult.cast();

            int flags = 0;
            if (x.isRight()) flags |= RelativeFlags.X;
            if (y.isRight()) flags |= RelativeFlags.Y;
            if (z.isRight()) flags |= RelativeFlags.Z;
            if (yaw.isRight()) flags |= RelativeFlags.YAW;
            if (yaw.isRight()) flags |= RelativeFlags.PITCH;
            return new Result.Ok<>(new RelativePos(new Pos(
                    x.map(Function.identity(), Function.identity()),
                    y.map(Function.identity(), Function.identity()),
                    z.map(Function.identity(), Function.identity()),
                    yaw.map(Function.identity(), Function.identity()).floatValue(),
                    pitch.map(Function.identity(), Function.identity()).floatValue()
            ), flags));
        }

        @Override
        public @NotNull <D> Result<D> encode(@NotNull Transcoder<D> coder, @Nullable RelativePos value) {
            if (value == null) return new Result.Error<>("null");
            var list = coder.createList(5);
            list.add((value.relativeFlags & RelativeFlags.X) != 0
                    ? coder.createString("~" + value.inner.x())
                    : coder.createDouble(value.inner.x()));
            list.add((value.relativeFlags & RelativeFlags.Y) != 0
                    ? coder.createString("~" + value.inner.y())
                    : coder.createDouble(value.inner.y()));
            list.add((value.relativeFlags & RelativeFlags.Z) != 0
                    ? coder.createString("~" + value.inner.z())
                    : coder.createDouble(value.inner.z()));
            list.add((value.relativeFlags & RelativeFlags.YAW) != 0
                    ? coder.createString("~" + value.inner.yaw())
                    : coder.createFloat(value.inner.yaw()));
            list.add((value.relativeFlags & RelativeFlags.PITCH) != 0
                    ? coder.createString("~" + value.inner.pitch())
                    : coder.createFloat(value.inner.pitch()));
            return new Result.Ok<>(list.build());
        }

        // Right either means its relative
        private <D> Result<Either<Double, Double>> parseNumberOrString(@NotNull Transcoder<D> coder, @NotNull List<D> list, int index) {
            if (index < 0 || index >= list.size())
                return new Result.Ok<>(Either.right(0.0));
            var raw = list.get(index);
            if (coder.getDouble(raw) instanceof Result.Ok(Double number))
                return new Result.Ok<>(Either.left(number));
            if (coder.getString(raw) instanceof Result.Ok(String str)) {
                if ("~".equals(str))
                    return new Result.Ok<>(Either.right(0.0));
                if (str.startsWith("~"))
                    return new Result.Ok<>(Either.right(Double.parseDouble(str.substring(1))));
                return new Result.Ok<>(Either.left(Double.parseDouble(str)));
            }

            return new Result.Error<>("Coordinate must be number or string, but got " + raw);
        }
    }

}

package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;

public abstract class RelativePointStructCodec<P extends Point, T extends RelativePoint<P>> implements StructCodec<T> {

    public static final StructCodec<RelativeVec> VEC = new VecImpl();
    public static final StructCodec<RelativePos> POS = new PosImpl();

    protected static <D> Result<RelativeField> decodeField(Transcoder<D> coder, Transcoder.MapLike<D> map, String key) {
        if (map.hasValue(key)) {
            var result = map.getValue(key);
            if (!(result instanceof Result.Ok(D entry))) return result.cast();
            if (coder.getDouble(entry) instanceof Result.Ok(var number)) {
                return new Result.Ok<>(RelativeField.abs(number));
            }

            try {
                if (coder.getString(entry) instanceof Result.Ok(var str)) {
                    var relative = str.startsWith(RelativeField.PREFIX);
                    var number = Double.parseDouble(relative ? str.substring(1) : str);

                    return new Result.Ok<>(new RelativeField(number, relative));
                }

                return new Result.Error<>("Coordinate must be number or string, but got " + result);
            } catch (NumberFormatException _) {}
        }
        return new Result.Ok<>(RelativeField.ORIGIN);
    }

    private static class VecImpl extends RelativePointStructCodec<Vec, RelativeVec> {

        @Override
        public <D> Result<RelativeVec> decodeFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
            var xResult = decodeField(coder, map, "x");
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = decodeField(coder, map, "y");
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = decodeField(coder, map, "z");
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();

            int flags = 0;
            if (x.relative()) flags |= RelativeFlags.X;
            if (y.relative()) flags |= RelativeFlags.Y;
            if (z.relative()) flags |= RelativeFlags.Z;

            return new Result.Ok<>(new RelativeVec(new net.minestom.server.coordinate.Vec(x.value(), y.value(), z.value()), flags));
        }

        @Override
        public <D> Result<D> encodeToMap(Transcoder<D> coder, RelativeVec value, Transcoder.MapBuilder<D> map) {
            map.put("x", RelativeField.encode(coder, value.vec().x(), (value.flags() & RelativeFlags.X) != 0));
            map.put("y", RelativeField.encode(coder, value.vec().y(), (value.flags() & RelativeFlags.Y) != 0));
            map.put("z", RelativeField.encode(coder, value.vec().z(), (value.flags() & RelativeFlags.Z) != 0));
            return new Result.Ok<>(map.build());
        }
    }

    private static class PosImpl extends RelativePointStructCodec<Pos, RelativePos> {

        @Override
        public <D> Result<RelativePos> decodeFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
            var xResult = decodeField(coder, map, "x");
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = decodeField(coder, map, "y");
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = decodeField(coder, map, "z");
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();
            var yawResult = decodeField(coder, map, "yaw");
            if (!(yawResult instanceof Result.Ok(var yaw))) return yawResult.cast();
            var pitchResult = decodeField(coder, map, "pitch");
            if (!(pitchResult instanceof Result.Ok(var pitch))) return pitchResult.cast();

            int flags = 0;
            if (x.relative()) flags |= RelativeFlags.X;
            if (y.relative()) flags |= RelativeFlags.Y;
            if (z.relative()) flags |= RelativeFlags.Z;
            if (yaw.relative()) flags |= RelativeFlags.YAW;
            if (pitch.relative()) flags |= RelativeFlags.PITCH;

            return new Result.Ok<>(new RelativePos(new Pos(x.value(), y.value(), z.value(), (float) yaw.value(), (float) pitch.value()), flags));
        }

        @Override
        public <D> Result<D> encodeToMap(Transcoder<D> coder, RelativePos value, Transcoder.MapBuilder<D> map) {
            map.put("x", RelativeField.encode(coder, value.pos().x(), (value.flags() & RelativeFlags.X) != 0));
            map.put("y", RelativeField.encode(coder, value.pos().y(), (value.flags() & RelativeFlags.Y) != 0));
            map.put("z", RelativeField.encode(coder, value.pos().z(), (value.flags() & RelativeFlags.Z) != 0));
            map.put("yaw", RelativeField.encode(coder, value.pos().yaw(), (value.flags() & RelativeFlags.YAW) != 0));
            map.put("pitch", RelativeField.encode(coder, value.pos().pitch(), (value.flags() & RelativeFlags.PITCH) != 0));
            return new Result.Ok<>(map.build());
        }
    }
}

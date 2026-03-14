package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class RelativePointArrayCodec<P extends Point, T extends RelativePoint<P>> implements Codec<T> {

    public static final Codec<RelativeVec> VEC = new VecImpl();
    public static final Codec<RelativePos> POS = new PosImpl();

    protected static <D> Result<RelativeField> decodeField(Transcoder<D> coder, List<D> map, int index) {
        if (index < 0 || index >= map.size()) return new Result.Ok<>(RelativeField.ORIGIN);
        var entry = map.get(index);
        if (coder.getDouble(entry) instanceof Result.Ok(var number)) return new Result.Ok<>(RelativeField.abs(number));
        try {
            if (coder.getString(entry) instanceof Result.Ok(var str)) {
                var relative = str.startsWith(RelativeField.PREFIX);
                var number = Double.parseDouble(relative ? str.substring(1) : str);

                return new Result.Ok<>(new RelativeField(number, relative));
            }

            return new Result.Error<>("Coordinate must be number or string, but got " + entry);
        } catch (NumberFormatException _) {}
        return new Result.Ok<>(RelativeField.ORIGIN);
    }

    private static class VecImpl extends RelativePointArrayCodec<Vec, RelativeVec> {

        @Override
        public <D> Result<RelativeVec> decode(Transcoder<D> coder, @NotNull D value) {
            var listResult = coder.getList(value);
            if (!(listResult instanceof Result.Ok(var list))) return listResult.cast();

            var xResult = decodeField(coder, list, 0);
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = decodeField(coder, list, 1);
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = decodeField(coder, list, 2);
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();

            int flags = 0;
            if (x.relative()) flags |= RelativeFlags.X;
            if (y.relative()) flags |= RelativeFlags.Y;
            if (z.relative()) flags |= RelativeFlags.Z;

            return new Result.Ok<>(new RelativeVec(new Vec(x.value(), y.value(), z.value()), flags));
        }

        @Override
        public <D> Result<D> encode(Transcoder<D> coder, @Nullable RelativeVec value) {
            if (value == null) return new Result.Error<>("null");
            var list = coder.createList(3);
            list.add(RelativeField.encode(coder, value.vec().x(), (value.flags() & RelativeFlags.X) != 0));
            list.add(RelativeField.encode(coder, value.vec().y(), (value.flags() & RelativeFlags.Y) != 0));
            list.add(RelativeField.encode(coder, value.vec().z(), (value.flags() & RelativeFlags.Z) != 0));

            return new Result.Ok<>(list.build());
        }
    }

    private static class PosImpl extends RelativePointArrayCodec<Pos, RelativePos> {

        @Override
        public <D> Result<RelativePos> decode(Transcoder<D> coder, @NotNull D value) {
            var listResult = coder.getList(value);
            if (!(listResult instanceof Result.Ok(var list))) return listResult.cast();

            var xResult = decodeField(coder, list, 0);
            if (!(xResult instanceof Result.Ok(var x))) return xResult.cast();
            var yResult = decodeField(coder, list, 1);
            if (!(yResult instanceof Result.Ok(var y))) return yResult.cast();
            var zResult = decodeField(coder, list, 2);
            if (!(zResult instanceof Result.Ok(var z))) return zResult.cast();
            var yawResult = decodeField(coder, list, 3);
            if (!(yawResult instanceof Result.Ok(var yaw))) return yawResult.cast();
            var pitchResult = decodeField(coder, list, 4);
            if (!(pitchResult instanceof Result.Ok(var pitch))) return pitchResult.cast();

            int flags = 0;
            if (x.relative()) flags |= RelativeFlags.X;
            if (y.relative()) flags |= RelativeFlags.Y;
            if (z.relative()) flags |= RelativeFlags.Z;
            if (yaw.relative()) flags |= RelativeFlags.YAW;
            if (pitch.relative()) flags |= RelativeFlags.PITCH;

            return new Result.Ok<>(new RelativePos(new Pos(
                    x.value(), y.value(), z.value(),
                    (float) yaw.value(), (float) pitch.value()
            ), flags));
        }

        @Override
        public <D> Result<D> encode(Transcoder<D> coder, @Nullable RelativePos value) {
            if (value == null) return new Result.Error<>("null");
            var list = coder.createList(5);
            list.add(RelativeField.encode(coder, value.pos().x(), (value.flags() & RelativeFlags.X) != 0));
            list.add(RelativeField.encode(coder, value.pos().y(), (value.flags() & RelativeFlags.Y) != 0));
            list.add(RelativeField.encode(coder, value.pos().z(), (value.flags() & RelativeFlags.Z) != 0));
            list.add(RelativeField.encode(coder, value.pos().yaw(), (value.flags() & RelativeFlags.YAW) != 0));
            list.add(RelativeField.encode(coder, value.pos().pitch(), (value.flags() & RelativeFlags.PITCH) != 0));

            return new Result.Ok<>(list.build());
        }
    }
}

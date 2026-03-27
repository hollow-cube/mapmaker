package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.RelativeFlags;
import org.intellij.lang.annotations.MagicConstant;

import java.util.function.DoubleFunction;

@SuppressWarnings("UnstableApiUsage")
public record RelativePos(
        Pos pos,
        @MagicConstant(flagsFromClass = RelativeFlags.class) int flags
) implements RelativePoint<Pos> {

    public static final StructCodec<RelativePos> STRUCT_CODEC = RelativePointStructCodec.POS;
    public static final Codec<RelativePos> ARRAY_CODEC = RelativePointArrayCodec.POS;

    public static final RelativePos ORIGIN = new RelativePos(Pos.ZERO, RelativeFlags.COORD | RelativeFlags.VIEW);

    public static RelativePos abs(Pos pos) {
        return new RelativePos(pos, RelativeFlags.NONE);
    }

    @Override
    public String x() {
        return RelativeField.toString(this.pos.x(), (this.flags & RelativeFlags.X) != 0);
    }

    @Override
    public String y() {
        return RelativeField.toString(this.pos.y(), (this.flags & RelativeFlags.Y) != 0);
    }

    @Override
    public String z() {
        return RelativeField.toString(this.pos.z(), (this.flags & RelativeFlags.Z) != 0);
    }

    public String yaw() {
        return RelativeField.toString(this.pos.yaw(), (this.flags & RelativeFlags.YAW) != 0);
    }

    public String pitch() {
        return RelativeField.toString(this.pos.pitch(), (this.flags & RelativeFlags.PITCH) != 0);
    }

    @Override
    public RelativePos withX(String x) {
        return this.with(x, RelativeFlags.X, this.pos::withX);
    }

    @Override
    public RelativePos withY(String y) {
        return this.with(y, RelativeFlags.Y, this.pos::withY);
    }

    @Override
    public RelativePos withZ(String z) {
        return this.with(z, RelativeFlags.Z, this.pos::withZ);
    }

    public RelativePos withYaw(String yaw) {
        return this.with(yaw, RelativeFlags.YAW, it -> this.pos.withYaw((float) it));
    }

    public RelativePos withPitch(String pitch) {
        return this.with(pitch, RelativeFlags.PITCH, it -> this.pos.withPitch((float) it));
    }

    @Override
    public Pos resolve(Pos origin) {
        double x = (this.flags & RelativeFlags.X) != 0 ? this.pos.x() + origin.x() : this.pos.x();
        double y = (this.flags & RelativeFlags.Y) != 0 ? this.pos.y() + origin.y() : this.pos.y();
        double z = (this.flags & RelativeFlags.Z) != 0 ? this.pos.z() + origin.z() : this.pos.z();
        float yaw = (this.flags & RelativeFlags.YAW) != 0 ? this.pos.yaw() + origin.yaw() : this.pos.yaw();
        float pitch = (this.flags & RelativeFlags.PITCH) != 0 ? this.pos.pitch() + origin.pitch() : this.pos.pitch();

        return new Pos(x, y, z, yaw, pitch);
    }

    private RelativePos with(
            String input,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flag,
            DoubleFunction<Pos> updater
    ) {
        int flags = this.flags & ~flag;
        if (input.startsWith(RelativeField.PREFIX)) {
            flags |= flag;
            input = input.substring(1);
        }
        double value = Double.parseDouble(input);
        if (Double.isInfinite(value) || Double.isNaN(value)) throw new NumberFormatException("Infinite or NaN value");
        return new RelativePos(updater.apply(value), flags);
    }
}

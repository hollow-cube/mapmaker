package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;
import org.intellij.lang.annotations.MagicConstant;

import java.util.function.DoubleFunction;

@SuppressWarnings("UnstableApiUsage")
public record RelativeVec(
        Vec vec,
        @MagicConstant(flagsFromClass = RelativeFlags.class) int flags
) implements RelativePoint<Vec> {

    public static final StructCodec<RelativeVec> STRUCT_CODEC = RelativePointStructCodec.VEC;
    public static final Codec<RelativeVec> ARRAY_CODEC = RelativePointArrayCodec.VEC;

    public static RelativeVec abs(Vec vec) {
        return new RelativeVec(vec, RelativeFlags.NONE);
    }

    @Override
    public String x() {
        return RelativeField.toString(this.vec.x(), (this.flags & RelativeFlags.X) != 0);
    }

    @Override
    public String y() {
        return RelativeField.toString(this.vec.y(), (this.flags & RelativeFlags.Y) != 0);
    }

    @Override
    public String z() {
        return RelativeField.toString(this.vec.z(), (this.flags & RelativeFlags.Z) != 0);
    }

    @Override
    public RelativeVec withX(String x) {
        return this.with(x, RelativeFlags.X, this.vec::withX);
    }

    @Override
    public RelativeVec withY(String y) {
        return this.with(y, RelativeFlags.Y, this.vec::withY);
    }

    @Override
    public RelativeVec withZ(String z) {
        return this.with(z, RelativeFlags.Z, this.vec::withZ);
    }

    @Override
    public Vec resolve(Vec origin) {
        double x = (this.flags & RelativeFlags.X) != 0 ? this.vec.x() + origin.x() : this.vec.x();
        double y = (this.flags & RelativeFlags.Y) != 0 ? this.vec.y() + origin.y() : this.vec.y();
        double z = (this.flags & RelativeFlags.Z) != 0 ? this.vec.z() + origin.z() : this.vec.z();

        return new Vec(x, y, z);
    }

    private RelativeVec with(
            String input,
            @MagicConstant(flagsFromClass = RelativeFlags.class) int flag,
            DoubleFunction<Vec> updater
    ) {
        int flags = this.flags & ~flag;
        if (input.startsWith(RelativeField.PREFIX)) {
            flags |= flag;
            input = input.substring(1);
        }
        return new RelativeVec(updater.apply(Double.parseDouble(input)), flags);
    }
}

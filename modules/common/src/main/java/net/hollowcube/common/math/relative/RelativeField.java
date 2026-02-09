package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.utils.Either;

public record RelativeField(double value, boolean relative) {

    public static final String PREFIX = "~";
    public static final RelativeField ORIGIN = RelativeField.rel(0.0);
    public static final RelativeField ZERO = RelativeField.abs(0.0);
    public static final Codec<RelativeField> CODEC = Codec.Either(Codec.STRING, Codec.DOUBLE).transform(
        it -> it.unify(RelativeField::fromString, RelativeField::abs),
        field -> field.relative ? Either.left(field.toString()) : Either.right(field.value)
    );

    public static RelativeField rel(double value) {
        return new RelativeField(value, true);
    }

    public static RelativeField abs(double value) {
        return new RelativeField(value, false);
    }

    public static RelativeField fromString(String str) {
        var relative = str.startsWith(RelativeField.PREFIX);
        var number = Double.parseDouble(relative ? str.substring(1) : str);
        return new RelativeField(number, relative);
    }

    public static String toString(double value, boolean relative) {
        return relative ? RelativeField.PREFIX + value : String.valueOf(value);
    }

    public static <D> D encode(Transcoder<D> coder, double value, boolean relative) {
        return relative ? coder.createString(RelativeField.PREFIX + value) : coder.createDouble(value);
    }

    public double resolve(double base) {
        return this.relative ? base + this.value : this.value;
    }

    @Override
    public String toString() {
        return RelativeField.toString(this.value, this.relative);
    }

    public String toDisplayString() {
        if (value == 0) {
            return this.relative ? "~" : "0";
        }
        return this.toString();
    }
}
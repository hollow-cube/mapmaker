package net.hollowcube.common.math.relative;

import net.minestom.server.codec.Transcoder;

@SuppressWarnings("UnstableApiUsage")
public record RelativeField(double value, boolean relative) {

    public static final String PREFIX = "~";
    public static final RelativeField ORIGIN = RelativeField.rel(0.0);
    public static final RelativeField ZERO = RelativeField.abs(0.0);

    public static RelativeField rel(double value) {
        return new RelativeField(value, true);
    }

    public static RelativeField abs(double value) {
        return new RelativeField(value, false);
    }

    public static String toString(double value, boolean relative) {
        return relative ? RelativeField.PREFIX + value : String.valueOf(value);
    }

    public static <D> D encode(Transcoder<D> coder, double value, boolean relative) {
        return relative ? coder.createString(RelativeField.PREFIX + value) : coder.createDouble(value);
    }

    @Override
    public String toString() {
        return RelativeField.toString(this.value, this.relative);
    }
}
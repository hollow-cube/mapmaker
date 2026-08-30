package net.hollowcube.anticheat.protocol;

/// The relative-movement flag bits sent as a big-endian int by `Relative.SET_STREAM_CODEC`.
///
/// The set is carried around as the raw packed int rather than an enum set: unpacking and
/// repacking would drop bits the client would still have seen, and the capture has to re-encode
/// byte for byte.
public final class Relative {

    public static final int X = 1;
    public static final int Y = 1 << 1;
    public static final int Z = 1 << 2;
    public static final int Y_ROT = 1 << 3;
    public static final int X_ROT = 1 << 4;
    public static final int DELTA_X = 1 << 5;
    public static final int DELTA_Y = 1 << 6;
    public static final int DELTA_Z = 1 << 7;
    public static final int ROTATE_DELTA = 1 << 8;

    public static boolean isSet(int relatives, int flag) {
        return (relatives & flag) != 0;
    }

    private Relative() {}
}

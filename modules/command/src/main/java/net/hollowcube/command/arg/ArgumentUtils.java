package net.hollowcube.command.arg;

public class ArgumentUtils {

    private ArgumentUtils() {}

    public static byte createNumberFlags(boolean min, boolean max) {
        byte i = 0;
        if (min) {
            i |= 1;
        }
        if (max) {
            i |= 2;
        }
        return i;
    }

}

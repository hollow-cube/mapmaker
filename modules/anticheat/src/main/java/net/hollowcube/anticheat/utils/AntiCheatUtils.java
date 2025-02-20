package modules.anticheat.src.main.java.net.hollowcube.anticheat.utils;

public final class AntiCheatUtils {

    /**
     * Compares two double values with a given precision.
     * We do it like this as a cheat can send slight changes in the player's position to bypass the anti-cheat.
     */
    public static boolean areSimilar(double a, double b, int precision) {
        if (a == b) return true;
        return Math.abs(a - b) < Math.pow(10, -precision);
    }
}

package net.hollowcube.common.util;

public class MathUtil {

    public static double toDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? fallback : parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

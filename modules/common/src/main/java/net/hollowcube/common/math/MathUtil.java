package net.hollowcube.common.math;

public final class MathUtil {

    public static float invsqrt(float r) {
        return 1.0F / (float) java.lang.Math.sqrt((double) r);
    }

    public static float cosFromSin(float sin, float angle) {
        float cos = (float) java.lang.Math.sqrt(1.0F - sin * sin);
        float a = angle + ((float) java.lang.Math.PI / 2F);
        float b = a - (float) ((int) (a / ((float) java.lang.Math.PI * 2F))) * ((float) java.lang.Math.PI * 2F);
        if ((double) b < (double) 0.0F) {
            b += ((float) java.lang.Math.PI * 2F);
        }

        return b >= (float) java.lang.Math.PI ? -cos : cos;
    }

    public static double parseFiniteDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? fallback : parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}

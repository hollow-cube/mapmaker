package net.hollowcube.mql.jit;

import net.hollowcube.mql.foreign.Query;

public interface ValueScript {

    class Variables {
        public double age;
        public double lifetime;
        public double random1;
        public double random2;
        public double random3;
        public double random4;
        public double particleRandom1;
        public double particleRandom2;
        public double particleRandom3;
        public double particleRandom4;

        @Query
        public double age() {
            return age;
        }

        @Query
        public double lifetime() {
            return lifetime;
        }

        @Query("random_1")
        public double random1() {
            return random1;
        }

        @Query("random_2")
        public double random2() {
            return random2;
        }

        @Query("random_3")
        public double random3() {
            return random3;
        }

        @Query("random_4")
        public double random4() {
            return random4;
        }

        @Query
        public double particle_random_1() {
            return particleRandom1;
        }

        @Query
        public double particle_random_2() {
            return particleRandom2;
        }

        @Query
        public double particle_random_3() {
            return particleRandom3;
        }

        @Query
        public double particle_random_4() {
            return particleRandom4;
        }

    }

    class Queries {
        public static final Queries INSTANCE = new Queries();

        private Queries() {
        }

        @Query("hsb_to_red")
        public double hsb_to_red(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 1 -> brightness * (1.0f - saturation * f);
                case 2, 3 -> brightness * (1.0f - saturation);
                case 4 -> brightness * (1.0f - (saturation * (1.0f - f)));
                default -> brightness;
            };
        }

        @Query("hsb_to_green")
        public double hsb_to_green(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 0 -> brightness * (1.0f - (saturation * (1.0f - f)));
                case 3 -> brightness * (1.0f - saturation * f);
                case 4, 5 -> brightness * (1.0f - saturation);
                default -> brightness;
            };
        }

        //todo mql apparently doesnt correctly use renames for methods.
        @Query("hsb_to_blue")
        public double hsb_to_blue(double hue, double saturation, double brightness) {
            if (saturation == 0) return brightness;
            double h = (hue - Math.floor(hue)) * 6.0f;
            double f = h - Math.floor(h);
            return switch ((int) h) {
                case 0, 1 -> brightness * (1.0f - saturation);
                case 2 -> brightness * (1.0f - (saturation * (1.0f - f)));
                case 5 -> brightness * (1.0f - saturation * f);
                default -> brightness;
            };
        }
    }

    double eval(
            @MqlEnv({"query", "q"}) Queries queries,
            @MqlEnv({"variable", "v"}) Variables variables
    );

}

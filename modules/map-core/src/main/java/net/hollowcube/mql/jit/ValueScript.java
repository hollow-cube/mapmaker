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
    }

    double eval(@MqlEnv({"variable", "v"}) Variables variables);

}

package net.hollowcube.terraform;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;

/// Flat test instances with their chunks loaded.
public final class TestInstances {

    /// A flat instance with the 2x2 chunk block `(0..1, 0..1)` loaded.
    public static Instance flat(Env env) {
        return flat(env, 0, 1, 0, 1);
    }

    /// A flat instance with the single chunk `(0, 0)` loaded.
    public static Instance flatChunk(Env env) {
        return flat(env, 0, 0, 0, 0);
    }

    /// A flat instance with every chunk in the inclusive chunk-coordinate box loaded.
    public static Instance flat(Env env, int minCx, int maxCx, int minCz, int maxCz) {
        Instance instance = env.createFlatInstance();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) instance.loadChunk(cx, cz).join();
        }
        return instance;
    }

    private TestInstances() {}
}

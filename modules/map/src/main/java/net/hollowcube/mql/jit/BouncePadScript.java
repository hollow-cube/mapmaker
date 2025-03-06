package net.hollowcube.mql.jit;

import net.hollowcube.mapmaker.map.feature.play.vanilla.ElytraFeatureProvider;
import net.hollowcube.mql.foreign.Query;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface BouncePadScript {

    class Variables {

        public double x;
        public double y;
        public double z;
        public double dx;
        public double dy;
        public double dz;
        public double yaw;
        public double pitch;

        @Query
        public double x() {
            return x;
        }

        @Query
        public double y() {
            return y;
        }

        @Query
        public double z() {
            return z;
        }

        @Query
        public double dx() {
            return dx;
        }

        @Query
        public double dy() {
            return dy;
        }

        @Query
        public double dz() {
            return dz;
        }

        @Query
        public double yaw() {
            return yaw;
        }

        @Query
        public double pitch() {
            return pitch;
        }

    }

    class Queries {

        public @Nullable Player player;

        @Query
        public double isSneaking() {
            return player != null && player.getPose() == EntityPose.SNEAKING ? 1 : 0;
        }

        @Query
        public double isSwimming() {
            return player != null && player.getPose() == EntityPose.SWIMMING ? 1 : 0;
        }

        @Query
        public double isSprinting() {
            return player != null && player.isSprinting() ? 1 : 0;
        }

        @Query
        public double isGliding() {
            return player != null && player.hasTag(ElytraFeatureProvider.IS_GLIDING_TAG) ? 1 : 0;
        }


    }

    double eval(
            @MqlEnv({"query", "q"}) Queries queries,
            @MqlEnv({"variable", "v"}) Variables variables
    );

}

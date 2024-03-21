package net.hollowcube.mapmaker.map.util;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Adds some functions for configuring per-player visibility. Will be implemented by a
 * {@link net.minestom.server.network.PlayerProvider}, so should be cast from players with error checking.
 */
public interface PlayerVisibilityExtension {

    enum Visibility {
        VISIBLE,
        /**
         * Makes them invisible (subject to team settings for ghost state)
         */
        INVISIBLE,
        /**
         * Puts their gamemode into spectator mode for everyone else
         */
        SPECTATOR;
        public static final Visibility[] VALUES = values();
    }

    void updateVisibility();

    void setVisibilityFunc(@Nullable Function<Player, Visibility> func);
}

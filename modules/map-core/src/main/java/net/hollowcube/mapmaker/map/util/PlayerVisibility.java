package net.hollowcube.mapmaker.map.util;

public enum PlayerVisibility {
    VISIBLE,
    /**
     * Makes them invisible (subject to team settings for ghost state)
     */
    INVISIBLE,
    /**
     * Puts their gamemode into spectator mode for everyone else
     */
    SPECTATOR;
    public static final PlayerVisibility[] VALUES = values();
}

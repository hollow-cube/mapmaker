package net.hollowcube.mapmaker.map;

/**
 * Controls how nearby players are rendered to the player.
 * <p>
 * todo currently we implement this with viewable rules, but perhaps we should just do it with invisibility & teams.
 */
public enum VisibilityRule {
    // Nearby players are turned into spectator ghosts
    GHOST,
    // Nearby players are hidden completely
    HIDDEN
}

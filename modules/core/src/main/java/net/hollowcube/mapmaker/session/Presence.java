package net.hollowcube.mapmaker.session;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record Presence(
        @NotNull String type,
        @NotNull String state,

        @NotNull String instanceId,
        @NotNull String mapId
) {

    public static final String TYPE_MAPMAKER_HUB = "mapmaker:hub";
    public static final String TYPE_MAPMAKER_MAP = "mapmaker:map";


    public static final Set<String> MAP_BUILDING_STATES = Set.of("editing", "testing", "verifying");
    public static final Set<String> MAP_PLAYING_STATES = Set.of("playing", "spectating");
}

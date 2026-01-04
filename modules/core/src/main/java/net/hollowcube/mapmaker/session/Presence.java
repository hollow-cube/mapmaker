package net.hollowcube.mapmaker.session;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@RuntimeGson
public record Presence(
        @NotNull String type,
        @NotNull String state,

        @NotNull String instanceId,
        @NotNull String mapId
) {

    public static final String TYPE_MAPMAKER_HUB = "mapmaker:hub";
    public static final String TYPE_MAPMAKER_MAP = "mapmaker:map";


    public static final Set<String> MAP_BUILDING_STATES = Set.of("editing", "testing");
    public static final Set<String> MAP_PLAYING_STATES = Set.of("playing", "spectating", "verifying");
}

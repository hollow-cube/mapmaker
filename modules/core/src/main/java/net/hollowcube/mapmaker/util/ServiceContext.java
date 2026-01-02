package net.hollowcube.mapmaker.util;

import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public record ServiceContext(
    PlayerService players,
    SessionService sessions,
    MapService maps,
    ServerBridge bridge
) {

}

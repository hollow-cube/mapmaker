package net.hollowcube.mapmaker.util;

import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import org.jetbrains.annotations.NotNullByDefault;

// TODO: long term this should just be ApiClient.
//  the 3 services are happening now, the bridge is a bit tricker but in the future should probably be managed by the service anyway.
@NotNullByDefault
public record ServiceContext(
    ApiClient api,
    PlayerService players,
    ServerBridge bridge
) {

}

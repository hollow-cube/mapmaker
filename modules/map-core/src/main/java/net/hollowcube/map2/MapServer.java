package net.hollowcube.map2;

import net.hollowcube.map.runtime.ServerBridge;
import net.hollowcube.map2.runtime.MapAllocator;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;

public interface MapServer {

    // Core services
    @NotNull SessionService sessionService();
    @NotNull PlayerService playerService();
    @NotNull MapService mapService();
    @NotNull PermManager permManager();
    @NotNull PlayerInviteService inviteService();

    // Higher level managers
    @NotNull MapAllocator allocator();
    @NotNull SessionManager sessionManager();
    @NotNull ServerBridge bridge();

    // Other
    <T> @NotNull T createInstance(@NotNull Class<T> type);

    // Map features
//    @NotNull List<FeatureProvider> features();
}

package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.find_a_new_home.legacy.LegacyMapService;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.service.PlayerService;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.world.WorldManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents the hub "server", even though it is not necessarily backed directly by a Minestom server.
 * It may also be loaded with a map server, etc.
 * <p>
 * Used internally to manage components of the MapMaker hub.
 */
public interface HubServer {

    class StaticAbuse {
        public static HubServer instance;
    }

    @NotNull HubToMapBridge bridge();

    @NotNull MapService mapService();





    @NotNull PlayerStorage playerStorage();

    @NotNull MetricStorage metricStorage();

    @NotNull WorldManager worldManager();

    @NotNull PlatformPermissionManager platformPermissions();

    @NotNull PlayerService playerService();

    @NotNull HubWorld world();

    @Nullable LegacyMapService legacyMapService();


    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}

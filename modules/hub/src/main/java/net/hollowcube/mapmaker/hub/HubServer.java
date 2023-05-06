package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.permission.MapPermissionManager;
import net.hollowcube.mapmaker.permission.PlatformPermissionManager;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.hollowcube.world.WorldManager;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents the hub "server", even though it is not necessarily backed directly by a Minestom server.
 * It may also be loaded with a map server, etc.
 * <p>
 * Used internally to manage components of the MapMaker hub.
 */
public interface HubServer {

    @NotNull HubToMapBridge bridge();

    @NotNull PlayerStorage playerStorage();

    @NotNull MapStorage mapStorage();

    @NotNull MetricStorage metricStorage();

    @NotNull WorldManager worldManager();

    @NotNull PlatformPermissionManager platformPermissions();

    @NotNull MapPermissionManager mapPermissions();

    @NotNull HubWorld world();


    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}

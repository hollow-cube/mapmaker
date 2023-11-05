package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents the hub "server", even though it is not necessarily backed directly by a Minestom server.
 * It may also be loaded with a map server, etc.
 * <p>
 * Used internally to manage components of the MapMaker hub.
 */
public interface HubServer {
    
    Tag<Boolean> DOUBLE_JUMP_TAG = Tag.Boolean("mapmaker:hub-double-jump").defaultValue(true);

    class StaticAbuse {
        public static HubServer instance;
    }

    @NotNull HubToMapBridge bridge();

    @NotNull PlayerService playerService();

    @NotNull SessionService sessionService();

    @NotNull MapService mapService();

    @NotNull PermManager permManager();


    @NotNull HubWorld world();

    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}

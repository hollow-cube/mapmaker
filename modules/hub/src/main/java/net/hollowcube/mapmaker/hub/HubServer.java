package net.hollowcube.mapmaker.hub;

import net.hollowcube.canvas.Section;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.hollowcube.mapmaker.storage.PlayerStorage;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the hub "server", even though it is not necessarily backed directly by a Minestom server.
 * It may also be loaded with a map server, etc.
 * <p>
 * Used internally to manage components of the MapMaker hub.
 */
public interface HubServer {

    @NotNull PlayerStorage playerStorage();

    @NotNull MapStorage mapStorage();

    @NotNull HubWorld world();

    void openGUIForPlayer(@NotNull Player player, @NotNull Section gui);

}

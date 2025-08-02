package net.hollowcube.mapmaker.map.util;

import net.hollowcube.command.util.CommandHandlingPlayer;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

/**
 * Implements some interfaces.
 * <p>
 * Overrides the following behavior:
 * - Always set listed to false on tab list entries. They will be managed by the session manager.
 */
public abstract class MapPlayerImpl extends CommandHandlingPlayer {
    public MapPlayerImpl(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

}

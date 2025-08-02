package net.hollowcube.mapmaker.map.util;

import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

// I(matt)DK what to name this class lol
public abstract class MapPlayerImplImpl extends MapPlayerImpl {

    public MapPlayerImplImpl(@NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }


}

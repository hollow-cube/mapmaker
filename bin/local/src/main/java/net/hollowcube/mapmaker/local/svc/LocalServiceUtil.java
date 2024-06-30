package net.hollowcube.mapmaker.local.svc;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class LocalServiceUtil {

    static @NotNull Player findPlayer(@NotNull String id) {
        var player = MinecraftServer.getConnectionManager().getConfigPlayers().stream()
                .filter(p -> p.getUuid().toString().equalsIgnoreCase(id))
                .findFirst().orElse(null);
        if (player != null) return player;

        player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(UUID.fromString(id));
        if (player != null) return player;

        throw new IllegalStateException("no player found for id: " + id);
    }

}

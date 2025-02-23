package net.hollowcube.mapmaker.local.svc;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.misc.noop.NoopSessionService;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.Presence;
import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class LocalSessionService extends NoopSessionService {

    @Override
    public @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String proxy, @NotNull String username, @NotNull String ip, @NotNull PlayerSkin skin) {
        throw new UnsupportedOperationException("i dont think i need this");
    }

    @Override
    public @NotNull TransferSessionResponse transferSession(@NotNull String id, @NotNull SessionTransferRequest req) {
        var player = MinecraftServer.getConnectionManager().getConfigPlayers().stream()
                .filter(p -> p.getUuid().toString().equalsIgnoreCase(id))
                .findFirst().orElse(null);
        Check.notNull(player, "no player in config");

        return new TransferSessionResponse(
                new PlayerDataV2(
                        id, player.getUsername(),
                        new DisplayName(List.of(new DisplayName.Part("username", player.getUsername(), null))),
                        new JsonObject(),
                        0, 0, 0
                ),
                new PlayerSession(
                        id, Instant.now(),
                        "local", "local",
                        false, player.getUsername(),
                        new PlayerSkin(player.getSkin()),
                        new Presence(Presence.TYPE_MAPMAKER_MAP, "editing", "local", "todo") //todo
                ),
                false
        );
    }

    @Override
    public void deleteSession(@NotNull String id) {
        throw new UnsupportedOperationException("i dont think i need this");
    }
}

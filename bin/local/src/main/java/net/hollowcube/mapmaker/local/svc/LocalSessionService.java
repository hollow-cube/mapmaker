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
    public @NotNull PlayerDataV2 createSessionV2(@NotNull String id, @NotNull SessionCreateRequestV2 body) {
        throw new UnsupportedOperationException("i dont think i need this");
//        return new PlayerDataV2(
//                id, body.username(),
//                new DisplayName(List.of(new DisplayName.Part("username", body.username(), null))),
//                new JsonObject(),
//                0, 0, 0
//        );
    }

    @Override
    public @NotNull TransferSessionResponse transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest req) {
        var player = MinecraftServer.getConnectionManager().getConfigPlayers().stream()
                .filter(p -> p.getUuid().toString().equalsIgnoreCase(id))
                .findFirst().orElse(null);
        Check.notNull(player, "no player in config");

        // @NotNull String playerId,
        //        @NotNull Instant createdAt,
        //
        //        @NotNull String proxyId,
        //        @NotNull String serverId,
        //
        //        boolean hidden,
        //        @NotNull String username,
        //        @NotNull PlayerSkin skin,
        //
        //        @UnknownNullability Presence presence

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
    public void deleteSessionV2(@NotNull String id) {
        throw new UnsupportedOperationException("i dont think i need this");
    }
}

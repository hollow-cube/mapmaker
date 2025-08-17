package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public class NoopSessionService implements SessionService {

    @Override
    public @NotNull List<PlayerSession> sync() {
        return List.of();
    }

    @Override
    public @NotNull PlayerData createSession(@NotNull String id, @NotNull String proxy, @NotNull String username, @NotNull String ip, @NotNull PlayerSkin skin) {
        return new PlayerData(
                id, username,
                new DisplayName(List.of(new DisplayName.Part("username", username, null))),
                new JsonObject(),
                0, 0, 0
        );
    }

    @Override
    public @NotNull TransferSessionResponse transferSession(@NotNull String id, @NotNull SessionTransferRequest req) {
        return new TransferSessionResponse(
                new PlayerData(
                        id, id,
                        new DisplayName(List.of(new DisplayName.Part("username", id, null))),
                        new JsonObject(),
                        0, 0, 0
                ),
                new PlayerSession(id, Instant.now(), 0, "noop-proxy-id", "noop-server-id", false, id, new PlayerSkin("", ""),
                        new Presence(req.type(), req.state(), req.server(), req.map())),
                false
        );
    }

    @Override
    public void deleteSession(@NotNull String id) {

    }

    @Override
    public @NotNull PlayerSession updateSessionProperties(@NotNull String playerId, @NotNull SessionStateUpdateRequest req) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull JoinMapResponse joinMapV2(@NotNull JoinMapRequest req) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull JoinMapResponse joinHubV2(@NotNull JoinHubRequest req) {
        return new JoinMapResponse("1", "2");
    }
}

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
    public boolean ready() {
        return true;
    }

    @Override
    public @NotNull List<PlayerSession> sync() {
        return List.of();
    }

    @Override
    public @NotNull PlayerDataV2 createSessionV2(@NotNull String id, @NotNull SessionCreateRequestV2 body) {
        return new PlayerDataV2(
                id, body.username(),
                new DisplayName(List.of(new DisplayName.Part("username", body.username(), null))),
                new JsonObject(),
                0, 0, 0
        );
    }

    @Override
    public @NotNull TransferSessionResponse transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest req) {
        return new TransferSessionResponse(
                new PlayerDataV2(
                        id, id,
                        new DisplayName(List.of(new DisplayName.Part("username", id, null))),
                        new JsonObject(),
                        0, 0, 0
                ),
                new PlayerSession(id, Instant.now(), "noop-proxy-id", "noop-server-id", false, id, new PlayerSkin("", ""),
                        new Presence(req.type(), req.state(), req.server(), req.map()))
        );
    }

    @Override
    public void deleteSessionV2(@NotNull String id) {

    }

    @Override
    public @NotNull PlayerSession updateSessionState(@NotNull String playerId, @NotNull SessionStateUpdateRequest req) {
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

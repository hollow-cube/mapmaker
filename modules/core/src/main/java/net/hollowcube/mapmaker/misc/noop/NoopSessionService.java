package net.hollowcube.mapmaker.misc.noop;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;

import java.time.Instant;
import java.util.List;

public class NoopSessionService implements SessionService {

    @Override
    public List<PlayerSession> sync() {
        return List.of();
    }

    @Override
    public PlayerData createSession(String id, String proxy, String username, String ip, PlayerSkin skin, String version, int protocolVersion) {
        return new PlayerData(
                id, username,
                new DisplayName(List.of(new DisplayName.Part("username", username, null))),
                new JsonObject(),
                0, 0, 0
        );
    }

    @Override
    public TransferSessionResponse transferSession(String id, SessionTransferRequest req) {
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
    public void deleteSession(String id) {

    }

    @Override
    public PlayerSession updateSessionProperties(String playerId, SessionStateUpdateRequest req) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JoinMapResponse joinMapV2(JoinMapRequest req) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JoinMapResponse joinHubV2(JoinHubRequest req) {
        return new JoinMapResponse("1", "2");
    }

    @Override
    public List<String> getIsolateOverrides() {
        return List.of();
    }
}

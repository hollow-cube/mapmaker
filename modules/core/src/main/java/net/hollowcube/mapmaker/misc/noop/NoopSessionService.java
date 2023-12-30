package net.hollowcube.mapmaker.misc.noop;

import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.PlayerSession;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoopSessionService implements SessionService {
    @Override
    public @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String username, @NotNull String ip) {
        return new PlayerDataV2(
                id, username,
                new DisplayName(List.of(new DisplayName.Part("username", username, null))),
                new PlayerSettings(),
                0, 0, 0
        );
    }

    @Override
    public void deleteSession(@NotNull String id) {

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
                new PlayerSettings(),
                0, 0, 0
        );
    }

    @Override
    public @NotNull PlayerDataV2 transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest req) {
        return new PlayerDataV2(
                id, id,
                new DisplayName(List.of(new DisplayName.Part("username", id, null))),
                new PlayerSettings(),
                0, 0, 0
        );
    }

    @Override
    public void deleteSessionV2(@NotNull String id) {

    }

    @Override
    public @NotNull JoinMapResponse joinMapV2(@NotNull JoinMapRequest req) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull JoinMapResponse joinHubV2(@NotNull JoinHubRequest req) {
        throw new UnsupportedOperationException();
    }
}

package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.player.*;
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
    public @NotNull PlayerDataV2 createSessionV2(@NotNull String id, @NotNull String username, @NotNull String ip) {
        return null;
    }

    @Override
    public @NotNull PlayerDataV2 transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest req) {
        return null;
    }

    @Override
    public void deleteSessionV2(@NotNull String id) {

    }
}

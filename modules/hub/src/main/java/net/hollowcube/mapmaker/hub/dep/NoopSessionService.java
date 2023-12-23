package net.hollowcube.mapmaker.hub.dep;

import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSettings;
import net.hollowcube.mapmaker.player.SessionService;
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
}

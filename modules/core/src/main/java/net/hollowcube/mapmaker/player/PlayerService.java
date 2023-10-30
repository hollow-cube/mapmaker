package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PlayerService {

    @NotNull DisplayName getPlayerDisplayName2(@NotNull String id);

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

    @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query);

}

package net.hollowcube.mapmaker.player;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PlayerService {

    @NotNull Component getPlayerDisplayName(@NotNull String id);

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

    @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query);

}

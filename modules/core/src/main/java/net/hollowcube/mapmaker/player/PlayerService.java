package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface PlayerService {

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

}

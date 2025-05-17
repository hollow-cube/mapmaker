package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record JoinHubRequest(
        @NotNull String player,
        @Nullable String exclude
) {
    public JoinHubRequest(@NotNull String player) {
        this(player, null);
    }

}

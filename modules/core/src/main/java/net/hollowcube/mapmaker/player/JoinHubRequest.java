package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record JoinHubRequest(
    String player,
    @Nullable String exclude
) {
    public JoinHubRequest(String player) {
        this(player, null);
    }

}

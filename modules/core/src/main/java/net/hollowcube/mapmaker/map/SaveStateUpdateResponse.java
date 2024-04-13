package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.player.AppliedRewards;
import org.jetbrains.annotations.Nullable;

public record SaveStateUpdateResponse(
        @Nullable AppliedRewards rewards,
        @Nullable Integer newPlacement
) {
}

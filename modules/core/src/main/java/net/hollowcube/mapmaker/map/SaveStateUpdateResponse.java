package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.AppliedRewards;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record SaveStateUpdateResponse(
        @Nullable AppliedRewards rewards,
        @Nullable Integer newPlacement
) {
}

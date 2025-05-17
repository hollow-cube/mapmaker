package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record PlayerDataUpdateMessage(
        @NotNull Action action,
        @NotNull String id,

        // Modify fields (all option, present should be applied)
        @Nullable JsonObject backpack, // Present fields will replace current value
        @Nullable Integer experience, // Replaces the current exp
        @Nullable Integer hypercubeExp, // Replaces the current hypercube exp
        @Nullable Integer coins, // Replaces the current coins
        @Nullable Integer cubits, // Replaces the current cubits

        // The reason for the update, or null if it is unspecified.
        @Nullable Reason reason
) {
    public enum Action {
        MODIFY
    }

    public enum ReasonType {
        CUBITS,
        HYPERCUBE,
        VOTE
    }

    @RuntimeGson
    public record Reason(
            @NotNull ReasonType type,
            Integer quantity, // Present for hypercube and cubits
            String voteSource, // Present for votes
            @Nullable PlayerDataUpdateMessage relativeUpdate // Present for votes
    ) {
    }
}

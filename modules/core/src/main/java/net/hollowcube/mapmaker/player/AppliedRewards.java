package net.hollowcube.mapmaker.player;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AppliedRewards(
        @NotNull Inventory diff,
        @Nullable Inventory newState
) {

    public record Inventory(
            @Nullable Integer coins,
            @Nullable Integer cubits,
            @Nullable Integer exp,
            @Nullable JsonElement backpack
    ) {

        public boolean hasCoins() {
            return coins != null && coins > 0;
        }

        public boolean hasCubits() {
            return cubits != null && cubits > 0;
        }

        public boolean hasExp() {
            return exp != null && exp > 0;
        }

    }
}

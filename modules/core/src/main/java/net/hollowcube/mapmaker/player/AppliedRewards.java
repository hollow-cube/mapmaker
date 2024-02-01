package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AppliedRewards {
    private @NotNull List<Entry> entries = new ArrayList<>();

    public AppliedRewards() {
        // GSON constructor
    }

    public AppliedRewards(@NotNull List<Entry> entries) {
        this.entries = entries;
    }

    public @NotNull List<Entry> entries() {
        return entries;
    }

    public record Entry(@NotNull RewardType type, @Nullable String id, int amount) {
        @Override
        public String id() {
            return id == null || id.isEmpty() ? null : id;
        }
    }

    @Override
    public String toString() {
        return "AppliedRewards{" +
                "entries=" + entries +
                '}';
    }
}

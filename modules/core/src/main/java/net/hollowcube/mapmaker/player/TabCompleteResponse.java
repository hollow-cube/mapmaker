package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public record TabCompleteResponse(
        @UnknownNullability List<Entry> result
) {

    public record Entry(@NotNull String id, @NotNull String username) {
    }


    @Override
    public @NotNull List<Entry> result() {
        return this.result == null ? List.of() : this.result;
    }


    public @NotNull List<Entry> resultSafe() {
        return this.result == null ? List.of() : this.result;
    }
}

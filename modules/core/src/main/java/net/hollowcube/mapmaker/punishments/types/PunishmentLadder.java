package net.hollowcube.mapmaker.punishments.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PunishmentLadder(@NotNull String id, @NotNull String name, @NotNull PunishmentType type,
                               @NotNull List<Entry> entries, @NotNull List<Reason> reasons) {

    public record Entry(long duration) {
    }

    public record Reason(@NotNull String id, @NotNull List<String> aliases) {
    }
}

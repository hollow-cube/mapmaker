package net.hollowcube.mapmaker.punishments;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PunishmentLadder(@NotNull String id, @NotNull String name, @NotNull PunishmentType type,
                               @NotNull List<Entry> entries) {

    public record Entry(long duration) {
    }
}

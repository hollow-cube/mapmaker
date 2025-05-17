package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record PunishmentLadder(@NotNull String id, @NotNull String name, @NotNull PunishmentType type,
                               @NotNull List<Entry> entries, @NotNull List<Reason> reasons) {

    @RuntimeGson
    public record Entry(long duration) {
    }

    @RuntimeGson
    public record Reason(@NotNull String id, @NotNull List<String> aliases) {
    }
}

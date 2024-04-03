package net.hollowcube.mapmaker.punishments.types;

import org.jetbrains.annotations.NotNull;

public record PunishmentUpdateMessage(@NotNull Action action, @NotNull Punishment punishment) {

    // Serialized as ordinal
    public enum Action {
        CREATE,
        REVOKE
    }
}

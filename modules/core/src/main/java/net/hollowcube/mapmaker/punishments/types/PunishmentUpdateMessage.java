package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record PunishmentUpdateMessage(@NotNull Action action, @NotNull Punishment punishment) {

    // Serialized as ordinal
    public enum Action {
        CREATE,
        REVOKE
    }
}

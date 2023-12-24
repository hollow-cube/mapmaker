package net.hollowcube.mapmaker.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public record SessionUpdateMessage(
        @NotNull Action action,
        @NotNull String playerId,

        @UnknownNullability PlayerSession session
) {

    public enum Action {
        CREATE,
        DELETE
    }

}

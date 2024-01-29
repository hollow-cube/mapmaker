package net.hollowcube.mapmaker.invite;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlayerInviteService {

    void join(@NotNull Player sender, @NotNull String targetId);

    void registerInvite(@NotNull Player sender, @NotNull String targetId);

    void registerRequest(@NotNull Player sender, @NotNull String targetId);

    void accept(@NotNull Player sender, @Nullable String targetId);

    default void accept(@NotNull Player sender) {
        this.accept(sender, null);
    }

    void reject(@NotNull Player sender, @Nullable String targetId);

    default void reject(@NotNull Player sender) {
        this.reject(sender, null);
    }

    class InternalError extends RuntimeException {
        public InternalError(@NotNull String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }
}

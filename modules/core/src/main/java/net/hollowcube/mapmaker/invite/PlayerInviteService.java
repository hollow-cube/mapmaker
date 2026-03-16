package net.hollowcube.mapmaker.invite;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface PlayerInviteService {

    void join(Player sender, String targetId);

    void registerInvite(Player sender, String targetId);

    void registerRequest(Player sender, String targetId);

    void accept(Player sender, @Nullable String targetId);

    default void accept(Player sender) {
        this.accept(sender, null);
    }

    void reject(Player sender, @Nullable String targetId);

    default void reject(Player sender) {
        this.reject(sender, null);
    }

    class InternalError extends RuntimeException {
        public InternalError(String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }
}

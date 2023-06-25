package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

@Blocking
public interface SessionService {

    @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String username, @NotNull String ip);
    void deleteSession(@NotNull String id);

    class InternalError extends RuntimeException {
        public InternalError(@NotNull String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }

}

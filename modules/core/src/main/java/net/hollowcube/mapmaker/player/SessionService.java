package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.util.GenericServiceError;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Blocking
public interface SessionService {

    @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String proxy, @NotNull String username, @NotNull String ip, @NotNull PlayerSkin skin);

    @NotNull TransferSessionResponse transferSession(@NotNull String id, @NotNull SessionTransferRequest req);

    void deleteSession(@NotNull String id);

    @NotNull PlayerSession updateSessionProperties(@NotNull String playerId, @NotNull SessionStateUpdateRequest req);

    @NotNull List<PlayerSession> sync();

    @NotNull JoinMapResponse joinMapV2(@NotNull JoinMapRequest req);

    @NotNull JoinMapResponse joinHubV2(@NotNull JoinHubRequest req);

    class InternalError extends RuntimeException {
        public InternalError(@NotNull String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }

    class UnauthorizedError extends RuntimeException {

        private final GenericServiceError error;

        public UnauthorizedError(@NotNull GenericServiceError error) {
            super(error.message());
            this.error = error;
        }

        public @NotNull GenericServiceError getError() {
            return error;
        }
    }

    class NoAvailableServerException extends RuntimeException {
        
    }

}

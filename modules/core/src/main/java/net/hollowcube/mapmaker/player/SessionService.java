package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Blocking
public interface SessionService {

    @Blocking
    boolean ready();

    @NotNull PlayerDataV2 createSession(@NotNull String id, @NotNull String username, @NotNull String ip);

    void deleteSession(@NotNull String id);

    @NotNull PlayerDataV2 createSessionV2(@NotNull String id, @NotNull SessionCreateRequestV2 body);

    @NotNull TransferSessionResponse transferSessionV2(@NotNull String id, @NotNull SessionTransferRequest req);

    void deleteSessionV2(@NotNull String id);

    @NotNull PlayerSession updateSessionState(@NotNull String playerId, @NotNull SessionStateUpdateRequest req);

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
    }

}

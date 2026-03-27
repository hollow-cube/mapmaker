package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.util.GenericServiceError;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Blocking
public interface SessionService {

    @NotNull PlayerData createSession(
        @NotNull String id,
        @NotNull String proxy,
        @NotNull String username,
        @NotNull String ip,
        @NotNull PlayerSkin skin,
        @NotNull String version,
        int protocolVersion
    );

    @NotNull TransferSessionResponse transferSession(@NotNull String id, @NotNull SessionTransferRequest req);

    void deleteSession(@NotNull String id);

    @NotNull PlayerSession updateSessionProperties(@NotNull String playerId, @NotNull SessionStateUpdateRequest req);

    @NotNull List<PlayerSession> sync();

    @NotNull JoinMapResponse joinMapV2(@NotNull JoinMapRequest req);

    @NotNull JoinMapResponse joinHubV2(@NotNull JoinHubRequest req);

    @NotNull List<String> getIsolateOverrides();

    default @NotNull JoinMapResponse findMapServer(@NotNull String mapId) {
        throw new UnsupportedOperationException("todo");
    }

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

        public UnauthorizedError(@Nullable GenericServiceError error) {
            super(error != null ? error.message() : null);
            this.error = error;
        }

        public @NotNull GenericServiceError getError() {
            return error;
        }
    }

    class SessionCreationDeniedError extends RuntimeException {

        private final String type;
        private final Component reason;

        public SessionCreationDeniedError(@NotNull String type, @NotNull String reason) {
            super(reason);
            this.type = type;
            this.reason = MiniMessage.miniMessage().deserialize(reason);
        }

        public @NotNull String type() {
            return type;
        }

        public @NotNull Component reason() {
            return reason;
        }
    }

    class NoAvailableServerException extends RuntimeException {

    }

}

package net.hollowcube.mapmaker.player;

import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.util.GenericServiceError;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Blocking
public interface SessionService {

    PlayerData createSession(
        String id,
        String proxy,
        String username,
        String ip,
        PlayerSkin skin,
        String version,
        int protocolVersion
    );

    TransferSessionResponse transferSession(String id, SessionTransferRequest req);

    void deleteSession(String id);

    PlayerSession updateSessionProperties(String playerId, SessionStateUpdateRequest req);

    List<PlayerSession> sync();

    JoinMapResponse joinMapV2(JoinMapRequest req);

    JoinMapResponse joinHubV2(JoinHubRequest req);

    List<String> getIsolateOverrides();

    default JoinMapResponse findMapServer(String mapId) {
        throw new UnsupportedOperationException("todo");
    }

    class InternalError extends RuntimeException {
        public InternalError(String message) {
            super(message);
        }

        public InternalError(Throwable cause) {
            super(cause);
        }
    }

    class UnauthorizedError extends RuntimeException {

        private final @Nullable GenericServiceError error;

        public UnauthorizedError(@Nullable GenericServiceError error) {
            super(error != null ? error.message() : null);
            this.error = error;
        }

        public @Nullable GenericServiceError getError() {
            return error;
        }
    }

    class SessionCreationDeniedError extends RuntimeException {

        private final String type;
        private final Component reason;

        public SessionCreationDeniedError(String type, String reason) {
            super(reason);
            this.type = type;
            this.reason = MiniMessage.miniMessage().deserialize(reason);
        }

        public String type() {
            return type;
        }

        public Component reason() {
            return reason;
        }
    }

    class NoAvailableServerException extends RuntimeException {

    }

}

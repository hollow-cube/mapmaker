package net.hollowcube.mapmaker.api;

import net.hollowcube.mapmaker.api.auth.AuthClient;
import net.hollowcube.mapmaker.api.hdb.HeadDatabaseRest;
import net.hollowcube.ipc.hdb.HeadDatabaseService;
import net.hollowcube.mapmaker.api.interaction.InteractionClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.notifications.NotificationClient;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.api.replays.ReplayClient;

import java.net.http.HttpResponse;

public final class ApiClient {

    public final PlayerClient players;
    public final MapClient maps;
    public final ReplayClient replays;
    public final HeadDatabaseService headDatabase;
    public final InteractionClient interactions;
    public final NotificationClient notifications;
    public final AuthClient auth;

    public ApiClient(
        PlayerClient players,
        MapClient maps,
        HeadDatabaseService headDatabase,
        InteractionClient interactions,
        NotificationClient notifications,
        AuthClient auth
    ) {
        this(
            players,
            maps,
            new ReplayClient.Noop(),
            headDatabase,
            interactions,
            notifications,
            auth
        );
    }

    public ApiClient(
        PlayerClient players,
        MapClient maps,
        ReplayClient replays,
        HeadDatabaseService headDatabase,
        InteractionClient interactions,
        NotificationClient notifications,
        AuthClient auth
    ) {
        this.players = players;
        this.maps = maps;
        this.replays = replays;
        this.headDatabase = headDatabase;
        this.interactions = interactions;
        this.notifications = notifications;
        this.auth = auth;
    }

    public ApiClient(HttpClientWrapper http) {
        this.players = new PlayerClient.Http(http);
        this.maps = new MapClient.Http(http);
        this.replays = new ReplayClient.Http(http);
        this.headDatabase = new HeadDatabaseRest(http);
        this.interactions = new InteractionClient.Http(http);
        this.notifications = new NotificationClient.Http(http);
        this.auth = new AuthClient.Http(http);
    }

    public static class Error extends RuntimeException {
        private final int statusCode;

        public Error(HttpResponse<?> response) {
            this.statusCode = response.statusCode();
        }

        public int statusCode() {
            return statusCode;
        }
    }

    public static class NotFoundError extends Error {
        public NotFoundError(HttpResponse<?> response) {
            super(response);
        }
    }

    public static class BadRequestError extends Error {
        public BadRequestError(HttpResponse<?> response) {
            super(response);
        }
    }

    public static class ConflictError extends Error {
        public ConflictError(HttpResponse<?> response) {
            super(response);
        }
    }

    public static class PreconditionFailedError extends Error {
        public PreconditionFailedError(HttpResponse<?> response) {
            super(response);
        }
    }

    public static class InternalServerError extends Error {
        public InternalServerError(HttpResponse<?> response) {
            super(response);
        }
    }

    public static RuntimeException notImplemented() {
        return new UnsupportedOperationException("Not implemented");
    }
}

package net.hollowcube.mapmaker.api;

import net.hollowcube.mapmaker.api.auth.AuthClient;
import net.hollowcube.ipc.chat.ChatService;
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
    public final ChatService chat;
    public final InteractionClient interactions;
    public final NotificationClient notifications;
    public final AuthClient auth;

    public ApiClient(
        PlayerClient players,
        MapClient maps,
        HeadDatabaseService headDatabase,
        ChatService chat,
        InteractionClient interactions,
        NotificationClient notifications,
        AuthClient auth
    ) {
        this(
            players,
            maps,
            new ReplayClient.Noop(),
            headDatabase,
            chat,
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
        ChatService chat,
        InteractionClient interactions,
        NotificationClient notifications,
        AuthClient auth
    ) {
        this.players = players;
        this.maps = maps;
        this.replays = replays;
        this.headDatabase = headDatabase;
        this.chat = chat;
        this.interactions = interactions;
        this.notifications = notifications;
        this.auth = auth;
    }

    /// Everything the Go api-server still serves comes off `http`; everything the java api-server
    /// serves is an ipc client built against its own base url, and so is passed in.
    public ApiClient(HttpClientWrapper http, HeadDatabaseService headDatabase, ChatService chat) {
        this.players = new PlayerClient.Http(http);
        this.maps = new MapClient.Http(http);
        this.replays = new ReplayClient.Http(http);
        this.headDatabase = headDatabase;
        this.chat = chat;
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

package net.hollowcube.mapmaker.api;

import net.hollowcube.ipc.chat.ChatService;
import net.hollowcube.ipc.hdb.HeadDatabaseService;
import net.hollowcube.ipc.map.MapService;
import net.hollowcube.ipc.notification.NotificationService;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.ipc.player.SocialService;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.mapmaker.api.auth.AuthClient;
import net.hollowcube.mapmaker.api.interaction.InteractionClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.player.LocalPlayerService;

import java.net.http.HttpResponse;

public final class ApiClient {

    public final PlayerService players;
    public final SocialService social;
    /// The map reads and writes the java api-server serves; `maps` wraps what Go still does.
    public final MapService mapService;
    public final MapClient maps;
    public final ReplayService replays;
    public final HeadDatabaseService headDatabase;
    public final ChatService chat;
    public final InteractionClient interactions;
    public final NotificationService notifications;
    public final AuthClient auth;

    /// Everything the Go api-server still serves comes off `http`; everything the java api-server
    /// serves is an ipc client built against its own base url, and so is passed in.
    public ApiClient(HttpClientWrapper http, HeadDatabaseService headDatabase, ChatService chat,
                     ReplayService replays, MapService maps, PlayerService players, SocialService social,
                     NotificationService notifications) {
        this.players = new LocalPlayerService(players);
        this.social = social;
        this.mapService = maps;
        this.maps = new MapClient.Http(http, maps);
        this.replays = replays;
        this.headDatabase = headDatabase;
        this.chat = chat;
        this.interactions = new InteractionClient.Http(http);
        this.notifications = notifications;
        this.auth = new AuthClient.Http(http);
    }

    public static class Error extends RuntimeException {
        private final int statusCode;

        public Error(int statusCode) {
            this.statusCode = statusCode;
        }

        public Error(HttpResponse<?> response) {
            this.statusCode = response.statusCode();
        }

        public int statusCode() {
            return statusCode;
        }
    }

    public static class NotFoundError extends Error {
        public NotFoundError() {
            super(404);
        }

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

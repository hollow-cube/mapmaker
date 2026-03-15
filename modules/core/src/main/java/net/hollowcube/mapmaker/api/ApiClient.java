package net.hollowcube.mapmaker.api;

import net.hollowcube.mapmaker.api.hdb.HeadDatabaseClient;
import net.hollowcube.mapmaker.api.interaction.InteractionClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.notifications.NotificationClient;
import net.hollowcube.mapmaker.api.players.PlayerClient;

import java.net.http.HttpResponse;

public final class ApiClient {

    public final PlayerClient players;
    public final MapClient maps;
    public final HeadDatabaseClient headDatabase;
    public final InteractionClient interactions;
    public final NotificationClient notifications;

    public ApiClient(
        PlayerClient players,
        MapClient maps,
        HeadDatabaseClient headDatabase,
        InteractionClient interactions,
        NotificationClient notifications
    ) {
        this.players = players;
        this.maps = maps;
        this.headDatabase = headDatabase;
        this.interactions = interactions;
        this.notifications = notifications;
    }

    public ApiClient(HttpClientWrapper http) {
        this.players = new PlayerClient.Http(http);
        this.maps = new MapClient.Http(http);
        this.headDatabase = new HeadDatabaseClient.Http(http);
        this.interactions = new InteractionClient.Http(http);
        this.notifications = new NotificationClient.Http(http);
    }

    public static class Error extends RuntimeException {
        private final int statusCode;

        public Error(HttpResponse<String> response) {
            this.statusCode = response.statusCode();
        }
    }

    public static class NotFoundError extends Error {
        public NotFoundError(HttpResponse<String> response) {
            super(response);
        }
    }

    public static class BadRequestError extends Error {
        public BadRequestError(HttpResponse<String> response) {
            super(response);
        }
    }

    public static class InternalServerError extends Error {
        public InternalServerError(HttpResponse<String> response) {
            super(response);
        }
    }

    public static RuntimeException notImplemented() {
        return new UnsupportedOperationException("Not implemented");
    }
}

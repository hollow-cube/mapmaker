package net.hollowcube.mapmaker.api;

import net.hollowcube.mapmaker.api.hdb.HeadDatabaseClient;
import net.hollowcube.mapmaker.api.players.PlayerClient;

import java.net.http.HttpResponse;

public final class ApiClient {

    public final PlayerClient players;
    public final HeadDatabaseClient headDatabase;

    public ApiClient(
        PlayerClient players, HeadDatabaseClient headDatabase
    ) {
        this.players = players;
        this.headDatabase = headDatabase;
    }

    public ApiClient(HttpClientWrapper http) {
        this.players = new PlayerClient.Http(http);
        this.headDatabase = new HeadDatabaseClient.Http(http);
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

    public static class InternalServerError extends Error {
        public InternalServerError(HttpResponse<String> response) {
            super(response);
        }
    }

    public static RuntimeException notImplemented() {
        return new UnsupportedOperationException("Not implemented");
    }
}

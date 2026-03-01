package net.hollowcube.mapmaker.api.hdb;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
import net.hollowcube.mapmaker.api.PaginatedList;

import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

public interface HeadDatabaseClient {

    PaginatedList<HeadInfo> getHeads(String query, int page, int pageSize);

    PaginatedList<HeadInfo> getHeadsInCategory(String category, int page, int pageSize);

    record Http(HttpClientWrapper http) implements HeadDatabaseClient {
        private static final String V4_PREFIX = "/v4/internal/head-database";

        @Override
        public PaginatedList<HeadInfo> getHeads(String query, int page, int pageSize) {
            return http.get(
                "getHeads",
                V4_PREFIX + "/search" + query("page", page, "pageSize", pageSize, "query", query),
                new TypeToken<>() {});
        }

        @Override
        public PaginatedList<HeadInfo> getHeadsInCategory(String category, int page, int pageSize) {
            return http.get(
                "getHeadsInCategory",
                V4_PREFIX + "/" + category + query("page", page, "pageSize", pageSize),
                new TypeToken<>() {});
        }
    }

}

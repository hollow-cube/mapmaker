package net.hollowcube.mapmaker.api.hdb;

import com.google.gson.reflect.TypeToken;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.hdb.HeadDatabaseService;
import net.hollowcube.ipc.hdb.HeadInfo;
import net.hollowcube.mapmaker.api.HttpClientWrapper;

import static net.hollowcube.mapmaker.api.HttpClientWrapper.query;

/// The head database as the Go api-server still serves it, behind the ipc interface.
///
/// Here so that callers can be moved onto the ipc client before the Java api-server is deployed;
/// once it is, this is deleted and `HeadDatabaseServiceHttp` takes its place.
public record HeadDatabaseRest(HttpClientWrapper http) implements HeadDatabaseService {
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

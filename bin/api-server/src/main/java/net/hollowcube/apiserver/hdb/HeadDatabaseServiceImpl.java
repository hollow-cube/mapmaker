package net.hollowcube.apiserver.hdb;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.HeadDb;
import net.hollowcube.apiserver.db.HeadsQueries;
import net.hollowcube.ipc.Page;
import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.hdb.HeadDatabaseService;
import net.hollowcube.ipc.hdb.HeadInfo;
import net.hollowcube.sqlgen.runtime.TsQuery;

/// The head database, served out of Postgres.
///
/// A query with no words in it is a browse rather than a search, and answers a random page of the
/// whole table — which is what the head picker opens on.
public final class HeadDatabaseServiceImpl implements HeadDatabaseService {

    private final ApiDatabase db;

    public HeadDatabaseServiceImpl(ApiDatabase db) {
        this.db = db;
    }

    @Override
    public PaginatedList<HeadInfo> getHeads(String query, int page, int pageSize) {
        var paging = Page.of(page, pageSize);
        var tsquery = TsQuery.of(query);
        if (tsquery == null) {
            return PaginatedList.of(db.heads.getRandomHeads(paging.limit()),
                HeadsQueries.GetRandomHeadsRow::totalCount, row -> headInfo(row.headDb()));
        }
        return PaginatedList.of(db.heads.getHeadsWithSearch(tsquery, paging.limit(), paging.offset()),
            HeadsQueries.GetHeadsWithSearchRow::totalCount, row -> headInfo(row.headDb()));
    }

    @Override
    public PaginatedList<HeadInfo> getHeadsInCategory(String category, int page, int pageSize) {
        var paging = Page.of(page, pageSize);
        return PaginatedList.of(db.heads.getHeadsWithCategory(category, paging.limit(), paging.offset()),
            HeadsQueries.GetHeadsWithCategoryRow::totalCount, row -> headInfo(row.headDb()));
    }

    private static HeadInfo headInfo(HeadDb row) {
        return new HeadInfo(String.valueOf(row.id()), row.name(), row.category(), row.texture(), row.tags());
    }
}

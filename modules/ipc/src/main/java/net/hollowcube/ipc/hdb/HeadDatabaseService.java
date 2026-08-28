package net.hollowcube.ipc.hdb;

import net.hollowcube.ipc.PaginatedList;
import net.hollowcube.ipc.util.Ipc;

@Ipc
public interface HeadDatabaseService {

    PaginatedList<HeadInfo> getHeads(String query, int page, int pageSize);

    PaginatedList<HeadInfo> getHeadsInCategory(String category, int page, int pageSize);

}

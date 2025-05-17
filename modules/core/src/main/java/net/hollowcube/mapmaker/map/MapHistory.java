package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record MapHistory(
        int page,
        boolean nextPage,
        @NotNull List<Entry> results
) {

    @RuntimeGson
    public record Entry(
            @NotNull String mapId
    ) {

    }
}

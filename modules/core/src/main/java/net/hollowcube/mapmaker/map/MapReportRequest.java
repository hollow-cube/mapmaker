package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public record MapReportRequest(
        @NotNull String reporter,
        @NotNull List<ReportCategory> categories,
        @Nullable String comment,
        @Nullable Pos location,
        @Nullable JsonObject context
) {
}

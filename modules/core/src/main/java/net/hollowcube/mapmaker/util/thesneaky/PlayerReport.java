package net.hollowcube.mapmaker.util.thesneaky;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@RuntimeGson
public record PlayerReport(
        @NotNull String playerId,
        @NotNull String brand,
        @NotNull Map<String, ModReportStatus> entries,
        boolean aModClearedTranslations
) {
}
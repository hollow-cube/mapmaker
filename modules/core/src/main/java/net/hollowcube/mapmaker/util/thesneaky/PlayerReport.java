package net.hollowcube.mapmaker.util.thesneaky;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record PlayerReport(
        @NotNull String playerId,
        @NotNull String brand,
        @NotNull Map<String, ModReportStatus> entries,
        boolean aModClearedTranslations
) {
}
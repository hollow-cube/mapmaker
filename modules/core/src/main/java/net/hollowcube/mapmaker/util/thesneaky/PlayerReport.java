package net.hollowcube.mapmaker.util.thesneaky;

import net.hollowcube.common.util.RuntimeGson;

import java.util.Map;

@RuntimeGson
public record PlayerReport(
    String playerId,
    String brand,
    Map<String, ModReportStatus> entries,
    boolean aModClearedTranslations
) {
}
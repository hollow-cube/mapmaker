package net.hollowcube.mapmaker.hub.feature.contest;

import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MapContest implements HubFeature {
    public static final String CONTEST_ID = "c9354e33-96c2-414a-9f4a-8c2ff4669086";
    public static final LocalDateTime START_DATE = LocalDateTime.parse("2025-06-21T10:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    public static final LocalDateTime END_DATE = LocalDateTime.parse("2025-06-21T10:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {

    }

}

package net.hollowcube.mapmaker.hub.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class HubTime {

    public static LocalDateTime now() {
        // TODO change timezone
        return LocalDateTime.now(ZoneId.of("America/New_York"));
    }
}

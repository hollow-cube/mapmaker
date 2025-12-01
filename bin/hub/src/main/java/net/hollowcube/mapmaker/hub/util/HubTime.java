package net.hollowcube.mapmaker.hub.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;


public class HubTime {

    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(-17);

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_OFFSET);
    }
}

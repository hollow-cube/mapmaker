package net.hollowcube.mapmaker.util;

import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.map.*;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyMapDataTest {
    private static final String IDS = "\"id\":\"00000000-0000-0000-0000-000000000001\",\"owner\":\"00000000-0000-0000-0000-000000000002\"";

    @Test
    void goSearchDecodesIntoTheSharedRecord() {
        var map = AbstractHttpService.GSON.fromJson("{" + IDS + """
            ,"protocolVersion":774,"settings":{"name":"Search result","icon":"minecraft:stone",
            "size":"large","variant":"parkour","tags":["future_tag"],"extra":{"no_jump":true},
            "leaderboard":{"asc":false,"format":"number","score":"q.score"}},
            "verification":"verified","publishedId":12345,"publishedAt":"2026-09-06T00:00:00Z",
            "quality":"great","difficulty":"hard","uniquePlays":123,"clearRate":0.3}
            """, MapData.class);
        assertEquals("Search result", map.name());
        assertEquals("000-012-345", map.publishedId());
        assertEquals(Instant.parse("2026-09-06T00:00:00Z"), map.publishedAt());
        assertEquals(MapDifficulty.HARD, map.difficulty());
        assertEquals(MapQuality.GREAT, map.quality());
        assertEquals(MapSize.LARGE, map.settings().size());
        assertEquals(List.of("future_tag"), map.settings().tags());
        assertTrue(map.settings().extra().get("no_jump").getAsBoolean());
        assertEquals(new MapLeaderboard(false, MapLeaderboard.Format.NUMBER, "q.score"), map.settings().leaderboard());
        assertEquals(map, Wire.gson().fromJson(Wire.gson().toJson(map), MapData.class));
    }

    @Test
    void absentGoFieldsUseDefaultsAndUnknownDifficultyMeansUnrated() {
        var map = AbstractHttpService.GSON.fromJson("{" + IDS + ",\"publishedId\":0,\"difficulty\":\"unknown\",\"settings\":{\"icon\":null,\"leaderboard\":null}}", MapData.class);
        assertFalse(map.isPublished());
        assertEquals(MapDifficulty.UNRATED, map.difficulty());
        assertEquals(MapData.Settings.defaults(), map.settings());
        assertEquals(MapData.DEFAULT_NAME, map.name());
    }

    @Test
    void unknownFutureEnumsKeepWireFallbacks() {
        var map = AbstractHttpService.GSON.fromJson("{" + IDS + ",\"difficulty\":\"future_difficulty\",\"quality\":\"future_quality\"}", MapData.class);
        assertEquals(MapDifficulty.UNKNOWN, map.difficulty());
        assertEquals(MapQuality.UNKNOWN, map.quality());
    }

    @Test
    void unratedSearchUsesTheGoEnumName() {
        assertTrue(MapSearchParams.builder().difficulties(MapDifficulty.UNRATED).build().toUrl("/search")
            .contains("difficulty=unknown"));
        assertTrue(MapSearchParams.builder().difficulties(MapDifficulty.HARD).build().toUrl("/search")
            .contains("difficulty=hard"));
    }
}

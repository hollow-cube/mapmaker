package net.hollowcube.mapmaker.util.gson;

import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.player.DisplayName;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPlayerDataDeserializerTest {
    @Test
    void goPlayerJsonDecodesToPlayerData() {
        for (var nameField : List.of("displayName", "displayNameV2")) {
            var info = AbstractHttpService.GSON.fromJson("""
                {
                  "id": "11111111-1111-1111-1111-111111111111", "username": "Alice",
                  "%s": [{"type": "username", "text": "Alice", "color": "#3895ff"}],
                  "settings": {"enabled": true}, "playtime": 1234,
                  "coins": 12, "cubits": 34, "hypercubeUntil": "2099-01-01T00:00:00Z",
                  "permissions": "18446744073709551615", "mapSlots": 5, "tempMaxMapSize": 4, "mapBuilders": 4
                }
                """.formatted(nameField), PlayerData.class);

            assertEquals(-1L, info.permissions());
            assertEquals(MapSize.UNLIMITED, info.maxMapSize());
            assertEquals(Instant.parse("2099-01-01T00:00:00Z"), info.hypercubeUntil());
            assertEquals("Alice", info.displayName().username());
            assertEquals(List.of(new DisplayName.Part.Username("Alice", "#3895ff")), info.displayName().parts());
            assertEquals(Component.text("Alice", TextColor.color(0x3895ff)),
                info.displayName().render(NamedTextColor.WHITE).compact());
        }
    }
}

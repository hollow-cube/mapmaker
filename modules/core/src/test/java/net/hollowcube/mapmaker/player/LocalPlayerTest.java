package net.hollowcube.mapmaker.player;

import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.ipc.player.PlayerData;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalPlayerTest {
    @Test
    void sessionStateUpdatesTheSharedSnapshot() {
        var info = AbstractHttpService.GSON.fromJson("""
            {
              "id": "11111111-1111-1111-1111-111111111111", "username": "Alice",
              "displayNameV2": [{"type": "username", "text": "Alice", "color": null}],
              "settings": {"enabled": true}, "playtime": 1234, "coins": 12, "cubits": 34,
              "permissions": "0", "mapSlots": 5, "tempMaxMapSize": 1, "mapBuilders": 4
            }
            """, PlayerData.class);
        var state = new LocalPlayer(info);
        assertSame(info, state.info());
        assertEquals(1234, state.storedPlaytime());
        assertEquals(12, state.coins());
        assertEquals(34, state.cubits());
        assertEquals(MapSize.LARGE, state.maxMapSize());
        assertFalse(state.isHypercube());

        state.setCoins(56);
        state.setCubits(78);
        assertEquals(56, state.info().coins());
        assertEquals(78, state.info().cubits());
        assertSame(info.displayName(), state.info().displayName());

        var enabled = PlayerSetting.Bool("enabled", false);
        assertTrue(state.getSetting(enabled));
        state.resetSetting(enabled);
        assertFalse(state.getSetting(enabled));
        assertFalse(state.info().settings().has("enabled"));
    }
}

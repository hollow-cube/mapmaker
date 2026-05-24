package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.test.AbstractScriptTest;
import net.hollowcube.mapmaker.test.TestScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibPlayersOnJoinTest extends AbstractScriptTest {

    @Test
    @TestScript("""
            local players = require("@mapmaker/players")
            players.on_join:connect(function(p)
                p:teleport(vec(10, 60, 10))
            end)
        """)
    void onJoin_handlerTeleportsJoiningPlayer() {
        assertEquals(10, player.getPosition().blockX());
        assertEquals(60, player.getPosition().blockY());
        assertEquals(10, player.getPosition().blockZ());
    }
}

package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.test.AbstractScriptTest;
import net.hollowcube.mapmaker.test.TestScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiFileRequireTest extends AbstractScriptTest {

    @Test
    @TestScript(path = "/lib/coords.luau", value = """
            return { home = vec(7, 50, 3) }
        """)
    @TestScript("""
            local coords = require("./lib/coords")
            local players = require("@mapmaker/players")
            players.on_join:connect(function(p)
                p:teleport(coords.home)
            end)
        """)
    void require_resolvesAndUsesHelperValue() {
        assertEquals(7, player.getPosition().blockX());
        assertEquals(50, player.getPosition().blockY());
        assertEquals(3, player.getPosition().blockZ());
    }
}

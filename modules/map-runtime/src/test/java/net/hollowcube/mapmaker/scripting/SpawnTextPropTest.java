package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.test.AbstractScriptTest;
import net.hollowcube.mapmaker.test.TestScript;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Regression test for the `WorldView:spawn_text_prop` slot-numbering bug that crashed the JVM
/// (SIGTRAP, exit 133) when the init table was read.
///
/// Root cause: `spawnTextProp` checked `state` at slot 1 (correct — namecall dispatch removes
/// `self` before invoking) but then called `tableForEach(state, 2, ...)`, `tableGet(state, 2, ...)`
/// throughout. `lua_next` on an unpopulated stack slot is undefined behavior in Luau and tripped
/// a debug assertion → SIGTRAP.
///
/// This test exercises the call site end-to-end through the real `ScriptEngine`: if the slot
/// numbering regresses, the JVM dies during the test run, which Gradle surfaces as a hard test
/// failure rather than a silent assertion. The `tableForEach` / `tableGet` helpers now also
/// validate the stack index up front and throw `IllegalArgumentException` instead of crashing,
/// giving a clear stack trace if a future caller makes the same mistake.
public class SpawnTextPropTest extends AbstractScriptTest {

    @Test
    @TestScript("""
            local players = require("@mapmaker/players")
            players.on_join:connect(function(p)
                p.world:spawn_text_prop({
                    position = vec(10, 60, 10),
                    text = "hello",
                    shadow = true,
                    background = 0,
                    billboard = "center",
                })
            end)
        """)
    void spawnTextPropDoesNotCrashOnInitTable() {
        // If the slot bug regresses, we never reach this line — the JVM exits with SIGTRAP
        // before assertions run. Reaching the assertions is itself the regression check.
        // Plus a sanity check that the entity actually landed in the instance, so a future
        // refactor that "silently passes" by skipping the spawn altogether also fails.
        var displayEntities = world.instance().getEntities().stream()
            .filter(DisplayEntity.Text.class::isInstance)
            .toList();
        assertEquals(1, displayEntities.size(),
            "expected exactly one text display spawned by the script — got " + displayEntities);
        var pos = displayEntities.getFirst().getPosition();
        assertEquals(10, pos.blockX());
        assertEquals(60, pos.blockY());
        assertEquals(10, pos.blockZ());
    }

    @Test
    @TestScript("""
            local players = require("@mapmaker/players")
            players.on_join:connect(function(p)
                local ok = pcall(function()
                    p.world:spawn_text_prop({
                        text = "missing position",
                    })
                end)
                assert(not ok, "expected spawn_text_prop to error without a position")
            end)
        """)
    void spawnTextPropMissingPositionErrorsCleanly() {
        // Pair: the missing-position path also indexed slot 2 in the original bug. Verifies
        // the argError call now surfaces as a catchable Lua error rather than a crash. We
        // don't introspect the error message here — Luau wraps errors as opaque userdata
        // whose textual content isn't easily queryable from Lua — `pcall` returning false
        // is enough to prove the path errors cleanly. The Java assertion confirms no entity
        // was accidentally spawned by the failing call.
        assertTrue(world.instance().getEntities().stream()
                .noneMatch(DisplayEntity.Text.class::isInstance),
            "no text display should have been spawned for the failing call");
    }
}

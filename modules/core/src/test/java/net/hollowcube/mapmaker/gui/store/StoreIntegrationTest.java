package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.misc.noop.NoopPermManager;
import net.hollowcube.mapmaker.misc.noop.NoopPlayerService;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
public class StoreIntegrationTest {

    @Test
    void sketching(Env env) throws Exception {
        var instance = env.createFlatInstance();
        var player = env.createPlayer(instance, new Pos(0, 40, 0));

        // Use the "prod" builtin module for the tests.
        var scriptEngine = new ScriptEngine(instance.scheduler(), new net.hollowcube.mapmaker.scripting.Env(false));
        StoreModule.openStoreView(scriptEngine, new NoopPlayerService(), new NoopPermManager(), player, null);

        env.tick();

        System.out.println(player.getOpenInventory());

    }
}

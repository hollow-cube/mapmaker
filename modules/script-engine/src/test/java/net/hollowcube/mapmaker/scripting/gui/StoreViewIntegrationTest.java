package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.coordinate.Pos;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
class StoreViewIntegrationTest {


    @Test
    void sketching(Env env) {
        var instance = env.createFlatInstance();
        var player = env.createPlayer(instance, new Pos(0, 40, 0));

        var scriptEngine = new ScriptEngine(instance.scheduler());


    }
}

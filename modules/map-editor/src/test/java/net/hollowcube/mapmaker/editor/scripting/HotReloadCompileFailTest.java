package net.hollowcube.mapmaker.editor.scripting;

import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HotReloadCompileFailTest extends AbstractMapWorldTest<TestMapWorld> {

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        return new TestMapWorld();
    }

    @Test
    void compileFailure_keepsPreviousScriptRunning() throws Exception {
        var source = new InMemoryScriptSource()
            .put("/main.luau", """
                    local players = require("@mapmaker/players")
                    players.on_join:connect(function(p)
                        p:teleport(vec(1, 50, 1))
                    end)
                """);

        var session = ReloadingScriptSession.reloading(source, "test-map");
        session.bootstrap();
        session.attach(world);

        env.tick();
        Player p1 = spawnTestPlayer(new Pos(0, 40, 0));
        env.tick();
        assertEquals(1, p1.getPosition().blockX(),
            "initial script should have teleported p1 to x=1");

        // Replace /main.luau with code that won't compile.
        source.put("/main.luau", "this is !! not valid luau");
        session.notifyFilesChanged(List.of("/main.luau"));

        // ReloadingScriptSession debounces by 150ms on a wall-clock
        // ScheduledExecutorService. Sleep past the debounce, then tick so the
        // rescheduled drainReload + runEntry rerun fire on the instance thread.
        for (int i = 0; i < 10; i++) {
            Thread.sleep(30);
            env.tick();
        }

        Player p2 = spawnTestPlayer(new Pos(0, 40, 0));
        env.tick();
        assertEquals(1, p2.getPosition().blockX(),
            "after a failed reload, the previous handler must still fire for p2");

        session.close();
    }
}

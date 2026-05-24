package net.hollowcube.mapmaker.test;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/// Ergonomic base for runtime/library/event tests. Reads [TestScript] from
/// the class and the running method, loads them into an [InMemoryModuleLoader],
/// then ticks once so the entry script executes and registers any handlers
/// before spawning a test player. Order matters: handlers like
/// `players.on_join` only see the join if they're registered first.
///
/// Defaults to a [TestMapWorld] - subclasses can override [#createWorld] to
/// run against a different concrete world.
///
/// Tests that need finer control (no annotation, multi-step loads, fault
/// injection) should extend [AbstractMapWorldTest] directly and drive
/// [ScriptEngine] themselves.
public abstract class AbstractScriptTest extends AbstractMapWorldTest<TestMapWorld> {

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        return new TestMapWorld();
    }

    protected ScriptEngine engine;
    protected Player player;

    @BeforeEach
    void loadScriptThenSpawn(TestInfo info) {
        var loader = new InMemoryModuleLoader();
        for (var s : collectScripts(info)) loader.put(s.path(), s.value());

        this.engine = new ScriptEngine(world, loader);

        // Tick once so ScriptEngine#runEntry executes and registers handlers
        // BEFORE the player spawns - handlers like `players.on_join` only see
        // the join if they're connected first. spawnTestPlayer dispatches its
        // own configure/spawn through the instance scheduler so the join event
        // fires on the same TickThread, satisfying the engine's thread-
        // confinement contract.
        env.tick();

        this.player = spawnTestPlayer();

        // Drain anything the join event scheduled (e.g. scheduleNextTick from
        // a signal callback).
        env.tick();
    }

    /// Class-level scripts first (treated as fixtures), then method-level.
    /// Same path declared twice: last one wins, matching
    /// [InMemoryModuleLoader#put]'s last-write semantics.
    private List<TestScript> collectScripts(TestInfo info) {
        var out = new ArrayList<TestScript>();
        for (Class<?> c = getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            addAll(out, c.getAnnotationsByType(TestScript.class));
        }
        Method method = info.getTestMethod().orElse(null);
        if (method != null) addAll(out, method.getAnnotationsByType(TestScript.class));
        return out;
    }

    private static void addAll(List<TestScript> out, TestScript[] arr) {
        for (var s : arr) out.add(s);
    }
}

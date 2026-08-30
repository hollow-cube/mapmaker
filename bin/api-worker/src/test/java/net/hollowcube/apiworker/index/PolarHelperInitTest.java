package net.hollowcube.apiworker.index;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// The indexer must not stand up a second server when it is loaded inside one.
///
/// [PolarHelper] needs the registries, and reaches for them through [MinecraftServer#init], which
/// builds a whole new `ServerProcess` and installs it. In a process already running a game server —
/// the development server hosts this indexer rather than running the worker beside it — that threw
/// the live process away: every event node registered on it went too, so players stopped being
/// given a spawning instance, and the server silently dropped to offline mode, which is what the
/// no-argument `init` defaults to.
class PolarHelperInitTest {

    @Test
    void ensureServerLeavesAProcessThatIsAlreadyRunningAlone() {
        // Called rather than merely loading the class: another test in this JVM may have loaded it
        // already, and a static initialiser only ever runs once, which would make this pass without
        // testing anything.
        PolarHelper.ensureServer();
        var server = MinecraftServer.process();
        assertNotNull(server, "the indexer should have brought up a process of its own");

        PolarHelper.ensureServer();

        assertSame(server, MinecraftServer.process(), "the running server process was replaced");
    }

    @Test
    void ensureServerBringsOneUpWhenThereIsNone() {
        // Whatever order the suite runs in, one of these two paths is the standalone worker's.
        PolarHelper.ensureServer();

        assertNotNull(MinecraftServer.process());
        assertSame(MinecraftServer.process(), MinecraftServer.process());
    }
}

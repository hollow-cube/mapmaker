package net.hollowcube.mapmaker.runtime.io;

import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class DispatchTest extends AbstractMapWorldTest<TestMapWorld> {
    private static final Key INPUT = Key.key("test:input");

    private record Probe(Consumer<PlayState> effect) implements PlayerInput {
        @Override
        public void applyTo(Player player, PlayState state) {
            effect.accept(state);
        }
    }

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        var world = new TestMapWorld();
        world.instance().loadChunk(0, 0).join();
        return world;
    }

    @Test
    void deliversOutputsInFireOrder() {
        var log = new ArrayList<String>();
        var dispatch = new Dispatch(spawnTestPlayer(), new PlayState());
        dispatch.fire("a", List.of(probe(_ -> log.add("a1")), probe(_ -> log.add("a2"))));
        dispatch.fire("b", List.of(probe(_ -> log.add("b1"))));
        assertTrue(log.isEmpty());

        dispatch.drain();
        assertEquals(List.of("a1", "a2", "b1"), log);
    }

    @Test
    void signalsFiredDuringTheDrainJoinTheSameLoop() {
        var log = new ArrayList<String>();
        var dispatch = new Dispatch(spawnTestPlayer(), new PlayState());
        dispatch.fire(
            "a",
            List.of(
                probe(_ -> {
                    log.add("a1");
                    dispatch.fire("nested", List.of(probe(_ -> log.add("nested"))));
                }),
                probe(_ -> log.add("a2"))
            )
        );

        dispatch.drain();
        assertEquals(List.of("a1", "a2", "nested"), log);
    }

    @Test
    void loopsStopAtTheSignalLimit() {
        var count = new int[1];
        var dispatch = new Dispatch(spawnTestPlayer(), new PlayState());
        var loop = new ArrayList<Connection>();
        loop.add(
            probe(_ -> {
                count[0]++;
                dispatch.fire("loop", loop);
            })
        );
        dispatch.fire("loop", loop);

        dispatch.drain();
        assertEquals(Dispatch.MAX_SIGNALS, count[0]);
    }

    @Test
    void aThrowingSignalIsSkippedAndTheRestStillRun() {
        var log = new ArrayList<String>();
        var dispatch = new Dispatch(spawnTestPlayer(), new PlayState());
        dispatch.fire(
            "a",
            List.of(
                probe(_ -> log.add("a1")),
                probe(_ -> {
                    throw new IllegalStateException("boom");
                }),
                probe(_ -> log.add("a3"))
            )
        );

        assertDoesNotThrow(dispatch::drain);
        assertEquals(List.of("a1", "a3"), log);
    }

    private static Connection probe(Consumer<PlayState> effect) {
        return new Connection(INPUT, new Probe(effect));
    }
}

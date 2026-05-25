package net.hollowcube.mapmaker.test;

import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;

@EnvTest
public abstract class AbstractMapWorldTest<W extends MapWorld> {

    static {
        MinecraftServer.init();
    }

    protected Env env;
    protected W world;

    protected abstract @NotNull W createWorld(@NotNull Env env);

    @BeforeEach
    void setupMapWorld(Env env) {
        this.env = env;

        DimensionTypes.register(env.process());

        PacketRegistryImpl.unsafeReset();
        PacketRegistryImpl.init(env.process().eventHandler());
        CompatProvider.load(env.process().eventHandler());

        // Override Minestom EnvImpl's default TestPlayerImpl with our MapPlayer
        // factory: AbstractMapWorld#spawnPlayer downcasts to MapPlayer, so the
        // upstream stand-in won't survive the cast. simpleMapPlayer is the
        // production "minimum viable" MapPlayer subclass.
        env.process().connection().setPlayerProvider(
            MapPlayer.simpleMapPlayer(new CommandManagerImpl()));

        this.world = createWorld(env);

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            world.safePointTick();
            return TaskSchedule.nextTick();
        });
    }

    protected @NotNull Player spawnTestPlayer(@NotNull Pos pos) {
        var player = env.createPlayer(world.instance(), pos);
        player.setTag(PlayerData.TAG, new PlayerData(player));

        // Schedule configure/spawn on the instance scheduler instead of running
        // them inline on the test thread. The instance tick (and anything it
        // drives - ScriptEngine#runEntry, Lua event handlers) runs on a
        // Minestom TickThread; firing world.spawnPlayer there too keeps every
        // Lua-touching path on the same thread, which is what the ScriptEngine
        // class-level "thread-confined" contract requires.
        world.scheduler().execute(() -> {
            world.configurePlayer(new AsyncPlayerConfigurationEvent(player, true));
            world.spawnPlayer(player);
        });
        env.tick();
        return player;
    }

    protected @NotNull Player spawnTestPlayer() {
        return spawnTestPlayer(new Pos(0, 40, 0));
    }
}

package net.hollowcube.mapmaker.editor;

import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.BeforeEach;

@EnvTest
public class AbstractEditorMapWorldIntegrationTest {

    static {
        MinecraftServer.init();
    }

    protected Env env;
    protected EditorMapWorld2 world;
    protected Player player;

    @BeforeEach
    void setup(Env env) {
        this.env = env;

        DimensionTypes.register(env.process());

        PacketRegistryImpl.unsafeReset();
        PacketRegistryImpl.init(env.process().eventHandler());
        CompatProvider.load(env.process().eventHandler());

        var map = new MapData();
        world = new EditorMapWorld2(null, map);

        player = env.createPlayer(world.instance(), new Pos(0, 40, 0));
        player.setTag(PlayerDataV2.TAG, new PlayerDataV2(player));

        world.configurePlayer(new AsyncPlayerConfigurationEvent(player, true));
        world.spawnPlayer(player);

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            world.safePointTick();
            return TaskSchedule.nextTick();
        });
    }

}

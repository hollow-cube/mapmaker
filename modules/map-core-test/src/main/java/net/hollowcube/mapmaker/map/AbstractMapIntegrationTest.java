package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.AbstractMapMakerMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@EnvTest
public abstract class AbstractMapIntegrationTest {
    static {
        System.setProperty("minestom.event.multiple-parents", "true");
    }

    protected Env env;
    protected MapWorld world;
    protected Player player;

    protected AbstractMapIntegrationTest() {

    }

    @BeforeEach
    void setup(Env env) {
        this.env = env;

        DimensionTypes.register(env.process());

        PacketRegistryImpl.unsafeReset();
        PacketRegistryImpl.init(env.process().eventHandler());
        CompatProvider.load(env.process().eventHandler());

        var server = new TestMapServer();
        var map = new MapData(UUID.randomUUID().toString(), UUID.randomUUID().toString(), new MapSettings(), 1, Instant.now());
        map.settings().setVariant(MapVariant.PARKOUR);
        // TODO: not valid to use playingmapworld here, should support others but not entirely sure the mechanism yet.
        world = new PlayingMapWorld(server, map);
        ((AbstractMapMakerMapWorld) world).load();

        player = env.createPlayer((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return new CommandManagerImpl();
            }
        }, world.instance(), new Pos(0, 40, 0));

        // String id, String username, DisplayName displayName, JsonObject settings, long playtime, int coins, int cubits
        player.setTag(PlayerData.TAG, new PlayerData(player.getUuid().toString(), player.getUsername(),
                new DisplayName(List.of(new DisplayName.Part("username", player.getUsername(), null))),
                new JsonObject(), 0, 0, 0));

        ((PlayingMapWorld) world).preAddPlayer(new AsyncPlayerConfigurationEvent(player, true));
        world.addPlayer(player);
        SaveState.fromPlayer(player).setPlayStartTime(System.currentTimeMillis());
    }
}

package net.hollowcube.mapmaker.map;

import com.google.gson.JsonObject;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.impl.PacketRegistryImpl;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ExtendWith(EnvTest.EnvParameterResolver.class)
@ExtendWith(EnvTest.EnvBefore.class)
@ExtendWith(EnvTest.EnvCleaner.class)
@ExtendWith(MapIntegrationTest.BeforeEachTest.class)
@ExtendWith(MapIntegrationTest.MapWorldParameterResolver.class)
@ExtendWith(MapIntegrationTest.PlayerParameterResolver.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MapIntegrationTest {

    final class BeforeEachTest implements BeforeEachCallback {
        static {
            System.setProperty("minestom.event.multiple-parents", "true");
        }

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            var env = EnvTest.EnvParameterResolver.get(context);

            PacketRegistryImpl.unsafeReset();
            PacketRegistryImpl.init(env.process().eventHandler());
            CompatProvider.load(env.process().eventHandler());
        }
    }

    final class MapWorldParameterResolver extends TypeBasedParameterResolver<MapWorld> {
        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MapWorldParameterResolver.class);

        @Override
        public MapWorld resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return get(extensionContext);

            // TODO: all kinds of state reset are required here.
        }

        public static @NotNull MapWorld get(ExtensionContext context) {
            return (MapWorld) context.getStore(NAMESPACE).getOrComputeIfAbsent(MapWorld.class, _ -> {
                var env = EnvTest.EnvParameterResolver.get(context);
                DimensionTypes.register(env.process());

                var server = new TestMapServer();
                var map = new MapData(UUID.randomUUID().toString(), UUID.randomUUID().toString(), new MapSettings(), 1, Instant.now());
                map.settings().setVariant(MapVariant.PARKOUR);

                // TODO: not valid to use playingmapworld here, should support others but not entirely sure the mechanism yet.
                var world = new PlayingMapWorld(server, map);
                world.load();
                return world;
            });
        }
    }

    final class PlayerParameterResolver extends TypeBasedParameterResolver<Player> {
        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PlayerParameterResolver.class);

        @Override
        public Player resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return get(extensionContext);
        }

        public static @NotNull Player get(ExtensionContext context) {
            return (Player) context.getStore(NAMESPACE).getOrComputeIfAbsent(Player.class, _ -> {
                var env = EnvTest.EnvParameterResolver.get(context);
                var world = MapWorldParameterResolver.get(context);
                var player = env.createPlayer((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
                    @Override
                    public @NotNull CommandManager getCommandManager() {
                        return new CommandManagerImpl();
                    }
                }, world.instance(), new Pos(0, 40, 0));

                // String id, String username, DisplayName displayName, JsonObject settings, long playtime, int coins, int cubits
                player.setTag(PlayerDataV2.TAG, new PlayerDataV2(player.getUuid().toString(), player.getUsername(),
                        new DisplayName(List.of(new DisplayName.Part("username", player.getUsername(), null))),
                        new JsonObject(), 0, 0, 0));

                ((PlayingMapWorld) world).preAddPlayer(new AsyncPlayerConfigurationEvent(player, true));
                world.addPlayer(player);
                SaveState.fromPlayer(player).setPlayStartTime(System.currentTimeMillis());
                return player;
            });
        }
    }
}

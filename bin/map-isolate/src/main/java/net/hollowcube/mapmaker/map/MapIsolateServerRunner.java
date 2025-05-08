package net.hollowcube.mapmaker.map;

import net.hollowcube.command.CommandManager;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.MapAllocator;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.AbstractMapMakerMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.misc.ProxySupport;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.player.JoinHubRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.Presence;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;

public class MapIsolateServerRunner extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateServerRunner.class);

    private final String mapId;

    private AbstractMapMakerMapWorld world;
    private FeatureList features;

    public MapIsolateServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        if (IsolateMain.args.length < 1)
            throw new IllegalArgumentException("Map ID must be provided as the last argument");
        this.mapId = UUID.fromString(IsolateMain.args[IsolateMain.args.length - 1]).toString();
        System.out.println("Map ID: " + this.mapId);
        System.out.println("Args: " + Arrays.toString(IsolateMain.args));

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("map-init")
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected @NotNull String name() {
        return "mapmaker-map-isolate";
    }

    @Override
    protected @NotNull MapAllocator createAllocator() {
        return MapAllocator.direct(this);
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return new ServerBridge() {
            @Override
            public void joinMap(@NotNull Player player, @NotNull String mapId, @NotNull JoinMapState joinMapState, @NotNull String source) {
                player.sendMessage("todo");
                return;
            }

            @Override
            public void joinHub(@NotNull Player player) {
                try {
                    var playerData = PlayerDataV2.fromPlayer(player);
                    var res = sessionService().joinHubV2(new JoinHubRequest(playerData.id()));
                    logger.info("join hub result: {}", res);
                    ProxySupport.transfer(player, res.serverClusterIp());
                } catch (Exception e) {
                    ExceptionReporter.reportException(e, player);
                    player.sendMessage(Component.text("An error occurred while trying to return to the hub. Please try again later."));
                }
            }
        };
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return commandManager();
            }
        });

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "headDatabase", "hdb");
        MapServerRunner.registerCommands(this, commandManager(), hdb);

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue("features", features::close);

        try {
            var map = mapService().getMap(Uuids.ZERO, this.mapId);
            world = allocator().allocateDirect(map, PlayingMapWorld.CTOR);
            shutdowner().queue("world", () -> world.close(null));

        } catch (Exception e) {
            logger.error("Error allocating map", e);
            throw new RuntimeException(e);
        }

        addBinding(Scheduler.class, world.instance().scheduler());
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();
            var playerId = player.getUuid().toString();

            // Queue resource pack download/apply while we do other things
            var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

            logger.info("configuring player {}", player.getUuid());
            var instanceId = ServerRuntime.getRuntime().hostname();
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, "playing", instanceId, this.mapId);
            transferPlayerSession(player, presence);

            // Ensure resource pack was applied before allowing the player in
            FutureUtil.getUnchecked(resourcePackFuture);
            if (!player.isOnline()) return;

            // Setup the player in the world
            world.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }
}

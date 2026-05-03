package net.hollowcube.mapmaker.isolate;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.MapCommands;
import net.hollowcube.mapmaker.api.maps.MapWorldMessage;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.session.Presence;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.hollowcube.mapmaker.map.MapPlayer.simpleMapPlayer;

public class MapIsolateServer extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapIsolateServer.class);

    private final String mapId;

    // Its only kinda unknown. it's not created in the constructor, but after prepareState
    // it is always not-null which should cover any reasonable logic.
    // TODO: pretty sure we could do init in constructor, should investigate.
    private @UnknownNullability ParkourMapWorld world;

    private final CompletableFuture<@Nullable Void> worldLoadFuture = new CompletableFuture<>();

    public MapIsolateServer(ConfigLoaderV3 config) {
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
    protected String name() {
        return "mapmaker-map-isolate";
    }

    public String mapId() {
        return mapId;
    }

    @Override
    protected ServerBridge createBridge() {
        return new MapIsolateBridge(api(), sessionService());
    }

    @Override
    public Scheduler scheduler() {
        return world.scheduler();
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager()
            .setPlayerProvider(simpleMapPlayer(commandManager()));
        MapCommands.registerPlayingCommands(this, commandManager());

        ParkourMapWorld.initGlobalReferences();

        try {
            var map = api().maps.get(this.mapId);

            world = new ParkourMapWorld(this, map);
            FutureUtil.submitVirtual(() -> {
                try {
                    world.loadWorld();
                } finally {
                    worldLoadFuture.complete(null);
                }
            });

            // We schedule on first tick end because submitTask invokes the executor immediately to determine
            // the first schedule. If we executed it here, that would be on the wrong thread.
            var scheduler = MinecraftServer.getSchedulerManager();
            scheduler.scheduleEndOfTick(() -> scheduler.submitTask(world::safePointTick, ExecutionType.TICK_END));
        } catch (Exception e) {
            logger.error("Error allocating map", e);
            throw new RuntimeException(e);
        }

        var serverId = ServerRuntime.getRuntime().hostname();
        var worldMessage = MapWorldMessage.created(serverId, mapId, "playing");
        jetStream.publish(worldMessage.subject(), worldMessage);

        var mapMgmtConsumer = new MapIsolateMapMgmtConsumerImpl(jetStream, this);
        shutdowner().queue("map-mgmt-listener", mapMgmtConsumer::close);
    }

    @Blocking
    public void shutdown(@Nullable Component reason) {
        FutureUtil.assertThread();

        var players = List.copyOf(world.players());
        var futures = new CompletableFuture[players.size()];
        for (int i = 0; i < players.size(); i++) {
            var player = players.get(i);
            if (reason != null) player.sendMessage(reason);
            futures[i] = world.scheduleRemovePlayer(player)
                .thenRunAsync(() -> bridge().joinHub(player), FutureUtil.VIRUTAL_EXECUTOR)
                .exceptionally(e -> {
                    ExceptionReporter.reportException(new RuntimeException("failed to remove player", e), player);
                    player.kick(Objects.requireNonNullElse(reason, Component.text("Server shutting down")));
                    return null;
                });
        }
        try {
            CompletableFuture.allOf(futures).get(15, TimeUnit.SECONDS);
        } catch (TimeoutException ignored) {
            logger.error("failed to drain players in 15s, exiting.");
        } catch (RuntimeException | InterruptedException | ExecutionException e) {
            logger.error("failed to drain players", e);
        }

        shutdowner().performShutdown();
    }

    protected void handleConfigPhase(AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();
            if (config.get(VelocityConfig.class).secret().isEmpty()) {
                ProtocolVersions.unsafeSetProtocolVersion(player, MinecraftServer.PROTOCOL_VERSION);
            } else {
                ProtocolVersions.requestProtocolVersionFromProxy(player);
                if (!player.isOnline()) return;
            }

            // Queue resource pack download/apply while we do other things
            var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

            logger.info("configuring player {}", player.getUuid());
            var instanceId = ServerRuntime.getRuntime().hostname();
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, "playing", instanceId, this.mapId);
            transferPlayerSession(player, presence);

            // Ensure resource pack was applied before allowing the player in
            FutureUtil.getUnchecked(resourcePackFuture);
            FutureUtil.getUnchecked(worldLoadFuture);
            if (!player.isOnline()) return;

            // Setup the player in the world
            world.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected void handleSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }

}

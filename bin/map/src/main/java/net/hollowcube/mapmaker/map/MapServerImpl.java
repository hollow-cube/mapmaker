package net.hollowcube.mapmaker.map;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import net.hollowcube.command.Command;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.dep.MapBridge;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.StandaloneServer;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.misc.noop.NoopPermManager;
import net.hollowcube.mapmaker.misc.noop.NoopPlayerService;
import net.hollowcube.mapmaker.misc.noop.NoopSessionService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.tag.Tag;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
class MapServerImpl extends MapServerBase implements StandaloneServer {
    private static final Logger logger = LoggerFactory.getLogger(MapServerImpl.class);

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();
    private static final GlobalEventHandler EVENT_HANDLER = MinecraftServer.getGlobalEventHandler();

    private static final long SHUTDOWN_MAX_WAIT_MILLIS = 10 * 1000; // 10 seconds

    private static final Tag<MapJoinInfo> JOIN_INFO_TAG = Tag.Transient("mapmaker:join_info");
    private static final Tag<MapWorld> TARGET_WORLD_TAG = Tag.Transient("mapmaker:target_world");

    private PlayerService playerService;
    private SessionService sessionService;
    private MapService mapService;
    private PermManager permManager;
    private MapToHubBridge bridge;

    private SessionManager sessionManager;

    private MapJoinConsumer mapJoinConsumer;

    private CommandManager commandManager;

    private boolean isReady = false; // Corresponds to readiness check
    private boolean isShuttingDown = false;

    private volatile CompletableFuture<Void> gracefulShutdownFuture = null;

    // A pending join is a map of player id to a future that will be completed when the server has the join info.
    // The future returned will never complete with an exception, but may complete with null indicating that we
    // timed out while waiting for the player info to be received.
    //
    // This will block the player from joining until the server has confirmed that it should have them and will
    // all happen during login (Minestom pre login event).
    // Note that we will separately hold them in the configuration phase until the map world is ready.
    private final Map<String, CompletableFuture<@Nullable MapJoinInfo>> pendingPlayerJoins = new ConcurrentHashMap<>();

    @Override
    public @Blocking void start(@NotNull ConfigLoaderV3 config) {
        var velocityConfig = config.get(VelocityConfig.class);
        if (!velocityConfig.secret().isEmpty()) {
            logger.info("Enabling modern forwarding...");
            VelocityProxy.enable(velocityConfig.secret());
        } else {
            logger.info("Velocity not configured, using online mode...");
            MojangAuth.init();
        }

        MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
        MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

        MinecraftServer.getSchedulerManager().buildShutdownTask(this::shutdown);

        // Service init
        boolean noopServices = Boolean.getBoolean("mapmaker.noop");

        var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
        if (playerServiceUrl != null) playerService = new PlayerServiceImpl(playerServiceUrl);
        else if (noopServices) playerService = new NoopPlayerService();
        else playerService = new PlayerServiceImpl("http://localhost:9126"); // tilt

        var sessionServiceUrl = System.getenv("MAPMAKER_SESSION_SERVICE_URL");
        if (sessionServiceUrl != null) sessionService = new SessionServiceImpl(sessionServiceUrl);
        else if (noopServices) sessionService = new NoopSessionService();
        else sessionService = new SessionServiceImpl("http://localhost:9127"); // tilt

        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl != null) mapService = new MapServiceImpl(mapServiceUrl);
        else if (noopServices) mapService = new NoopMapService();
        else mapService = new MapServiceImpl("http://localhost:9125"); // tilt

        permManager = new NoopPermManager();

        var kafkaConfig = config.get(KafkaConfig.class);
        sessionManager = new SessionManager(sessionService, playerService, kafkaConfig);

        mapJoinConsumer = new MapJoinConsumer(kafkaConfig.bootstrapServersStr());

        bridge = new MapBridge(sessionService);

        // Command init
        commandManager = new CommandManager();
        CommandHandlingPlayer.init();
        CONNECTION_MANAGER.setPlayerProvider(CommandHandlingPlayer.createDefaultProvider(commandManager));

        var cmd = new Command("sessions") {
        };
        cmd.setDefaultExecutor((sender, context) -> {
            sessionManager.sessions().forEach(session -> {
                sender.sendMessage(Component.text(session.playerId()));
            });
        });
        commandManager.register(cmd);

        // Standalone hub specific events
        EVENT_HANDLER
                .addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin)
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handlePlayerDisconnect)
                .addListener(PlayerPluginMessageEvent.class, this::handlePlayerPluginMessage);

        // The rest of the server init
        init(config, commandManager);
        isReady = true;
    }

    @Override
    public void handleHttpShutdown(@NotNull ServerRequest serverRequest, @NotNull ServerResponse serverResponse) {
        if (gracefulShutdownFuture != null) {
            gracefulShutdownFuture.thenRun(() -> serverResponse.status(200).send());
            return;
        }

        gracefulShutdownFuture = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(SHUTDOWN_MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS)
                // Automatically complete the future after the timeout.
                .execute(() -> gracefulShutdownFuture.complete(null));
        logger.info("received shutdown request");

        // At this point we have entered the pre shutdown hook for the pod. We have a maximum of
        // SHUTDOWN_MAX_WAIT_MILLIS to remove all players from the server then we will enter
        // the shutdown hook for the jvm (receive sigterm).
        Audiences.players().sendMessage(Component.text("Shutdown started (this message is temp)"));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        serverResponse.status(200).send();
        gracefulShutdownFuture.complete(null);
    }

    @Override
    public void shutdown() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        MinecraftServer.stopCleanly();
        sessionManager.close();
        mapJoinConsumer.close();
        super.shutdown();
    }

    @Override
    public @NotNull MapToHubBridge bridge() {
        return bridge;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
    }

    @Override
    public @NotNull SessionService sessionService() {
        return sessionService;
    }

    @Override
    public @NotNull MapService mapService() {
        return mapService;
    }

    @Override
    public @NotNull PermManager permManager() {
        return permManager;
    }

    @Override
    public @NotNull Collection<HealthCheck> readinessChecks() {
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"),
                () -> isReady ? HealthCheckResponse.up("hub") : HealthCheckResponse.down("hub"),
                () -> gracefulShutdownFuture == null ? HealthCheckResponse.up("graceful_shutdown") : HealthCheckResponse.down("graceful_shutdown")
        );
    }

    private @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId) {
        var noop = mapService instanceof NoopMapService;
        if (noop) {
            //todo
            return CompletableFuture.completedFuture(new MapJoinInfo(
                    playerId,
                    "62da0aaf-8cad-4c13-869c-02b07688988d",
                    "editing"
            ));
        }

        return pendingPlayerJoins.computeIfAbsent(playerId, id -> {
            var future = new CompletableFuture<MapJoinInfo>();
            //todo the futures are never actually removed from the map.
            // invalid to do below because it will remove the future before we can handle it in login event.
//            future.whenComplete((v, e) -> pendingPlayerJoins.remove(id));
            return future;
        });
    }

    private void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUuid().toString();

        try {
            var joinInfo = FutureUtil.getUnchecked(getPendingJoin(playerId));
            if (joinInfo == null) {
                logger.error("timed out waiting for join info for {}", playerId);
                player.kick(Component.text("Failed to join. Please try again later."));
                return;
            }

            logger.info("got join info for {}: {}", playerId, joinInfo);
            player.setTag(JOIN_INFO_TAG, joinInfo);
            var transferReq = new SessionTransferRequest(
                    AbstractHttpService.hostname,
                    Presence.TYPE_MAPMAKER_MAP,
                    joinInfo.state(),
                    joinInfo.mapId()
            );
            var playerData = sessionService.transferSessionV2(player.getUuid().toString(), transferReq);
            player.setTag(PlayerDataV2.TAG, playerData);

            var mapPlayerData = mapService.getMapPlayerData(playerData.id());
            player.setTag(MapPlayerData.TAG, mapPlayerData);
            logger.info("loaded map player data: {}", mapPlayerData);
//        } catch (SessionService.UnauthorizedError ignored) {
//            player.kick(Component.text("The server is currently in a closed beta.\nVisit ")
//                    .append(Component.text("hollowcube.net").clickEvent(ClickEvent.openUrl("https://hollowcube.net/")))
//                    .append(Component.text(" for more information.")));
        } catch (Exception e) {
            logger.error("failed to transfer session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
        }
    }

    private void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();

        // Apply the resource pack, making sure not to continue if apply fails.
//        ResourcePackManager.sendResourcePack(player).join();
//        if (!player.isOnline()) return;

        var joinInfo = player.getTag(JOIN_INFO_TAG);
        if (joinInfo == null) {
            logger.error("missing join info for {}", event.getPlayer().getUuid());
            player.kick(Component.text("Failed to join. Please try again later."));
            return;
        }
        player.removeTag(JOIN_INFO_TAG);

        // Create the world, holding the player here until it is ready for them to join.
        var map = mapService.getMap(joinInfo.playerId(), joinInfo.mapId());
        var pendingWorld = worldManager().getOrCreateMapWorld(map, Presence.MAP_BUILDING_STATES.contains(joinInfo.state())
                ? ServerBridge.JoinMapState.EDITING : ServerBridge.JoinMapState.PLAYING);
        var mapWorld = Objects.requireNonNull(FutureUtil.getUnchecked(pendingWorld));

        // Send the registry for the map. TODO this should only happen if it actually is required.
        // TODO: This should probably also be part of MapWorld?
        var registry = new HashMap<String, NBT>();
        registry.put("minecraft:chat_type", Messenger.chatRegistry());
        registry.put("minecraft:dimension_type", MinecraftServer.getDimensionTypeManager().toNBT());
        registry.put("minecraft:worldgen/biome", mapWorld.biomes().toNBT());
        registry.put("minecraft:damage_type", DamageType.getNBT());
        player.sendPacket(new RegistryDataPacket(NBT.Compound(registry)));
        player.sendPacket(new TagsPacket(MinecraftServer.getTagManager().getTagMap()));
        event.setSendRegistryData(false);

        // Spawn them into the world.
        player.setTag(TARGET_WORLD_TAG, mapWorld);
        event.setSpawningInstance(mapWorld.instance());
        player.setRespawnPoint(mapWorld.spawnPoint());
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        var playerData = PlayerDataV2.fromPlayer(player);

        // Player init
        player.setDisplayName(playerData.displayName());
        player.setTeam(CoreTeams.DEFAULT); // todo do this based on rank
        Emoji.sendTabCompletions(player);
        MiscFunctionality.sendBetaHeader(player);
        ActionBar.forPlayer(player).addProvider(MiscFunctionality::buildCurrencyDisplay);
        MiscFunctionality.broadcastTabList(player, sessionManager.networkPlayerCount());

        // Garbage below

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());

        var targetWorld = player.getTag(TARGET_WORLD_TAG);
        if (targetWorld != null) {
            player.removeTag(TARGET_WORLD_TAG);
            ((InternalMapWorld) targetWorld).acceptPlayer(player, true);
        }
    }

    private void handlePlayerDisconnect(@NotNull PlayerDisconnectEvent event) {
        logger.info("disconnect - {}", event.getPlayer().getUsername());
    }

    private void handlePlayerPluginMessage(@NotNull PlayerPluginMessageEvent event) {
        if (!event.getIdentifier().equals("mapmaker:transfer")) return;
        var player = event.getPlayer();

        // This is only sent when it is a failure.
        player.sendMessage(Component.text("failed to join map!"));
    }

    // {"server_id":"map-c89db8f95-g92xs","player_id":"aceb326f-da15-45bc-bf2f-11940c21780c","map_id":"ddd0419e-499c-4292-87af-411bbfb362d2","state":"editing"}
    private record MapJoinInfoMessage(@NotNull String serverId, @NotNull String playerId, @NotNull String mapId,
                                      @NotNull String state) {
    }

    private class MapJoinConsumer extends BaseConsumer<MapJoinInfoMessage> {

        protected MapJoinConsumer(@NotNull String bootstrapServers) {
            super("map-join", AbstractHttpService.hostname, s -> AbstractHttpService.GSON.fromJson(s, MapJoinInfoMessage.class), bootstrapServers);
        }

        @Override
        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull MapJoinInfoMessage message) {
            if (!AbstractHttpService.hostname.equals(message.serverId())) return; // Not for this server, ignore.

            logger.info("received join info for {}: {}", message.playerId(), message);
            var pendingJoin = getPendingJoin(message.playerId());
            pendingJoin.complete(new MapJoinInfo(message.playerId(), message.mapId(), message.state()));
        }
    }
}

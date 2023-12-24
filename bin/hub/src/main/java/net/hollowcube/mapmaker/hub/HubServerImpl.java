package net.hollowcube.mapmaker.hub;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.hub.dep.*;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapServiceImpl;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.ConnectionManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
class HubServerImpl extends HubServerBase {
    private static final Logger logger = LoggerFactory.getLogger(HubServerImpl.class);

    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();
    private static final GlobalEventHandler EVENT_HANDLER = MinecraftServer.getGlobalEventHandler();

    private PlayerService playerService;
    private SessionService sessionService;
    private MapService mapService;
    private PermManager permManager;
    private HubToMapBridge bridge;

    private SessionManager sessionManager;

    private CommandManager commandManager;

    private boolean isReady = false; // Corresponds to readiness check
    private boolean isShuttingDown = false;

    public @Blocking void start(@NotNull ConfigProvider config) {
//        var velocityConfig = config.get(VelocityConfig.class);
        var velocityConfig = new VelocityConfig(System.getenv("MAPMAKER_VELOCITY_SECRET"));
        if (velocityConfig.secret() != null && !velocityConfig.secret().isEmpty()) {
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

        playerService = new NoopPlayerService();
        sessionService = new NoopSessionService();

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

        sessionManager = new SessionManager(sessionService, playerService, new KafkaConfig("kafka:9092"));

        bridge = new NoopHubBridge();

        // Command init
        commandManager = new CommandManager();
        CommandHandlingPlayer.init();
        CONNECTION_MANAGER.setPlayerProvider(CommandHandlingPlayer.createDefaultProvider(commandManager));

        var cmd = new Command("sessions"){};
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
                .addListener(PlayerDisconnectEvent.class, this::handlePlayerDisconnect);

        // The rest of the server init
        init(commandManager, new NoopPlayerInviteService());
        isReady = true;
    }

    @Override
    public void shutdown() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        MinecraftServer.stopCleanly();
        sessionManager.close();
        super.shutdown();
    }

    @Override
    public @NotNull HubToMapBridge bridge() {
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

    public @NotNull Collection<HealthCheck> readinessChecks() {
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"),
                () -> isReady ? HealthCheckResponse.up("hub") : HealthCheckResponse.down("hub")
        );
    }

    private void handlePreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var player = event.getPlayer();

        try {
            var playerData = sessionService.transferSessionV2(player.getUuid().toString());
            player.setTag(PlayerDataV2.TAG, playerData);

            var mapPlayerData = mapService.getMapPlayerData(playerData.id());
            player.setTag(MapPlayerData.TAG, mapPlayerData);
            logger.info("loaded map player data: {}", mapPlayerData);
        } catch (SessionService.UnauthorizedError ignored) {
            player.kick(Component.text("The server is currently in a closed beta.\nVisit ")
                    .append(Component.text("hollowcube.net").clickEvent(ClickEvent.openUrl("https://hollowcube.net/")))
                    .append(Component.text(" for more information.")));
        } catch (Exception e) {
            logger.error("failed to create session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
        }
    }

    private void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        event.setSpawningInstance(instance());
        event.getPlayer().setRespawnPoint(HUB_SPAWN_POINT);
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

        // Send the join message for themselves only. For everyone else it will be sent whenever the session create message is received.
        player.sendMessage(Component.translatable("chat.player.join", playerData.displayName2().build()));
        MiscFunctionality.broadcastTabList(player, sessionManager.networkPlayerCount()); // Also send initial tablist

        // Garbage below

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());
    }

    private void handlePlayerDisconnect(@NotNull PlayerDisconnectEvent event) {
        logger.info("disconnect - {}", event.getPlayer().getUsername());
    }
}

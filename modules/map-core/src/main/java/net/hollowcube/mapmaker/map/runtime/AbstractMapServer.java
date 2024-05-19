package net.hollowcube.mapmaker.map.runtime;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ResourceAttributes;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.Injectors;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.chat.announcements.ChatAnnouncer;
import net.hollowcube.mapmaker.command.DiscordCommand;
import net.hollowcube.mapmaker.command.EmojisCommand;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.map.MapCommand;
import net.hollowcube.mapmaker.command.punish.*;
import net.hollowcube.mapmaker.command.staff.SFindCommand;
import net.hollowcube.mapmaker.command.staff.UnvanishCommand;
import net.hollowcube.mapmaker.command.staff.VanishCommand;
import net.hollowcube.mapmaker.command.store.StoreCommand;
import net.hollowcube.mapmaker.command.util.*;
import net.hollowcube.mapmaker.config.*;
import net.hollowcube.mapmaker.consumer.PlayerDataUpdateConsumer;
import net.hollowcube.mapmaker.cosmetic.CosmeticInventoryHandler;
import net.hollowcube.mapmaker.event.util.UpdateSignTextEvent;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.feature.unleash.UnleashFeatureFlagProvider;
import net.hollowcube.mapmaker.invite.MapInviteAcceptedOrRejectedListener;
import net.hollowcube.mapmaker.invite.MapInviteListener;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.invite.PlayerInviteServiceImpl;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.map.util.DynamicController;
import net.hollowcube.mapmaker.map.util.DynamicInjector;
import net.hollowcube.mapmaker.map.util.MapPlayerImpl;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.metrics.MetricWriterImpl;
import net.hollowcube.mapmaker.metrics.MetricWriterNoop;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.ExpBarRenderer;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.noop.*;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PermManagerImpl;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.punishments.PunishmentManagementListener;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.PunishmentServiceImpl;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.store.ShopUpgradeCache;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.ServerStatsHud;
import net.hollowcube.mapmaker.util.Shutdowner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractMapServer implements MapServer {
    private final Logger logger = LoggerFactory.getLogger(MapServer.class);

    static {
        var a = HCTypeRegistry.class; // Force init
    }

    protected final ConfigLoaderV3 config;
    protected final GlobalConfig globalConfig;

    private final MetricWriter metrics;
    private final SessionService sessionService;
    private final PlayerService playerService;
    private final MapService mapService;
    private final PermManager permManager;
    private final PunishmentService punishmentService;
    private PlayerInviteService inviteService; // So many dependencies very yikes

    // Listeners for other features
    private MapAllocator allocator;
    private ServerBridge bridge;

    private SessionManager sessionManager;
    private ChatMessageListener chatMessageListener;
    private MapInviteListener mapInviteListener;
    private MapInviteAcceptedOrRejectedListener mapInviteAcceptedOrRejectedListener;
    private PlayerDataUpdateConsumer playerDataUpdateConsumer;

    private final CommandManager commandManager = new CommandManagerImpl();

    private DynamicController guiController = new DynamicController();
    private DynamicInjector injector = new DynamicInjector();

    private volatile boolean isReady = false; // Corresponds to the associated health check
    private final Shutdowner shutdowner = new Shutdowner(this::awaitQuiescence);

    protected AbstractMapServer(@NotNull ConfigLoaderV3 config) {
        this.config = config;
        this.globalConfig = config.get(GlobalConfig.class);

        var otel = initTracing(config);

        var metricsConfig = config.get(MetricsConfig.class);
        if (metricsConfig.password() != null && !metricsConfig.password().isEmpty()) {
            this.metrics = new MetricWriterImpl(metricsConfig.password());
        } else this.metrics = new MetricWriterNoop();

        var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
        if (playerServiceUrl != null) {
            playerService = new PlayerServiceImpl(otel, playerServiceUrl);
            punishmentService = new PunishmentServiceImpl(playerServiceUrl);
        } else if (globalConfig.noop()) {
            playerService = new NoopPlayerService();
            punishmentService = new NoopPunishmentService();
        } else {
            var localUrl = "http://localhost:9126"; // tilt
            playerService = new PlayerServiceImpl(otel, localUrl);
            punishmentService = new PunishmentServiceImpl(localUrl);
        }

        var sessionServiceUrl = System.getenv("MAPMAKER_SESSION_SERVICE_URL");
        if (sessionServiceUrl != null) sessionService = new SessionServiceImpl(sessionServiceUrl);
        else if (globalConfig.noop()) sessionService = new NoopSessionService();
        else sessionService = new SessionServiceImpl("http://localhost:9127"); // tilt

        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl != null) mapService = new MapServiceImpl(mapServiceUrl);
        else if (globalConfig.noop()) mapService = new NoopMapService();
        else mapService = new MapServiceImpl("http://localhost:9125"); // tilt

        if (globalConfig.noop()) {
            permManager = new NoopPermManager();
        } else {
            var spicedbUrl = System.getenv("MAPMAKER_SPICEDB_URL");
            if (spicedbUrl == null) spicedbUrl = "localhost:50051";
            var spicedbToken = System.getenv("MAPMAKER_SPICEDB_TOKEN");
            if (spicedbToken == null) spicedbToken = "supersecretkey";
            permManager = new PermManagerImpl(spicedbUrl, spicedbToken);
        }

        ShopUpgradeCache.init(permManager);
    }

    protected abstract @NotNull String name();

    @Blocking
    public final void start() {
        // Minestom init bits
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
        MinecraftServer.getConnectionManager().setPlayerProvider((uuid, username, connection) -> new MapPlayerImpl(uuid, username, connection) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return commandManager;
            }
        });

        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setPlayListener(ClientUpdateSignPacket.class, UpdateSignTextEvent::packetListener);

        // Dependent service init

        var unleashConfig = config.get(UnleashConfig.class);
        if (unleashConfig.enabled()) {
            logger.info("Unleash is enabled, loading feature flag provider");
            var provider = new UnleashFeatureFlagProvider(unleashConfig, shutdowner);
            FeatureFlagProvider.replaceGlobals(provider);
        } else {
            FeatureFlagProvider.replaceGlobals((ignored1, ignored2) -> unleashConfig.defaultAction());
        }

        allocator = createAllocator();
        shutdowner.queue(allocator::close);
        bridge = createBridge();

        var kafkaConfig = config.get(KafkaConfig.class);
        sessionManager = new SessionManager(sessionService, playerService, permManager, kafkaConfig, globalConfig.noop());
        shutdowner.queue(sessionManager::close);
        sessionManager().sync(); // Sync existing sessions with remote

        // Must be initialized this late because of all its dependencies. this is pretty yikes im not a big fan
        var inviteServiceUrl = System.getenv("MAPMAKER_PLAYER_INVITE_SERVICE_URL");
        if (inviteServiceUrl != null)
            this.inviteService = new PlayerInviteServiceImpl(inviteServiceUrl, playerService, mapService, sessionManager, bridge, permManager);
        else if (globalConfig.noop()) this.inviteService = new NoopPlayerInviteService();
        else
            this.inviteService = new PlayerInviteServiceImpl("http://localhost:9127", playerService, mapService, sessionManager, bridge, permManager); // tilt

        if (!globalConfig.noop()) {
            mapInviteListener = new MapInviteListener(mapService, playerService, sessionManager, kafkaConfig.bootstrapServersStr());
            shutdowner.queue(mapInviteListener::close);

            mapInviteAcceptedOrRejectedListener = new MapInviteAcceptedOrRejectedListener(mapService, playerService, sessionManager, bridge(), kafkaConfig.bootstrapServersStr());
            shutdowner.queue(mapInviteAcceptedOrRejectedListener::close);

            var punishmentCreatedListener = new PunishmentManagementListener(playerService, permManager, kafkaConfig.bootstrapServersStr());
            shutdowner.queue(punishmentCreatedListener::close);

            chatMessageListener = new ChatMessageListener(sessionManager, playerService, mapService, punishmentService, kafkaConfig.bootstrapServersStr());
            injector.bind(ChatMessageListener.class, chatMessageListener);
            shutdowner.queue(chatMessageListener::close);
            packetListenerManager.setPlayListener(ClientChatMessagePacket.class, chatMessageListener);

            playerDataUpdateConsumer = new PlayerDataUpdateConsumer(kafkaConfig.bootstrapServersStr());
            shutdowner.queue(playerDataUpdateConsumer::close);
        }

        ChatAnnouncer.setupAnnouncements(config, sessionManager());

        injector.bind(Controller.class, guiController);
        prepareStart();

        var ignored = ObjectTypes.CHECKPOINT_PLATE; // Will fix when reworking ram usage

        // Finally, mark the service as ready for Kubernetes
        isReady = true;
    }

    @Override
    public @NotNull MetricWriter metrics() {
        return metrics;
    }

    @Override
    public @NotNull SessionService sessionService() {
        return sessionService;
    }

    @Override
    public @NotNull PlayerService playerService() {
        return playerService;
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
    public @NotNull PunishmentService punishmentService() {
        return punishmentService;
    }

    @Override
    public @NotNull PlayerInviteService inviteService() {
        return inviteService;
    }

    @Override
    public @NotNull MapAllocator allocator() {
        return allocator;
    }

    @Override
    public @NotNull ServerBridge bridge() {
        return bridge;
    }

    @Override
    public @NotNull SessionManager sessionManager() {
        return sessionManager;
    }

    public @NotNull CommandManager commandManager() {
        return commandManager;
    }

    @Override
    public @NotNull Controller guiController() {
        return guiController;
    }

    protected abstract @NotNull MapAllocator createAllocator();
    protected abstract @NotNull ServerBridge createBridge();

    /**
     * Called just before the server starts, but after all services have been initialized.
     */
    protected void prepareStart() {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        CosmeticInventoryHandler.init(guiController);

        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
        globalEventHandler.addChild(entityEvents);
        MapEntities.init(entityEvents);

        addBinding(MapServer.class, this, "mapServer", "server");
        addBinding(ConfigLoaderV3.class, config);

        addBinding(MetricWriter.class, metrics, "metrics");
        addBinding(SessionService.class, sessionService, "sessionService");
        addBinding(PlayerService.class, playerService, "playerService");
        addBinding(MapService.class, mapService, "mapService");
        addBinding(PermManager.class, permManager, "permManager");
        addBinding(PunishmentService.class, punishmentService, "punishmentService");
        addBinding(PlayerInviteService.class, inviteService, "playerInviteService");

        addBinding(MapAllocator.class, allocator, "allocator");
        addBinding(SessionManager.class, sessionManager, "sessionManager");
        addBinding(ServerBridge.class, bridge(), "bridge");

        commandManager.register(createInstance(MinestomCommand.class));
        commandManager.register(createInstance(EmojisCommand.class));
        commandManager.register(createDebugCommand());
        commandManager.register(createInstance(StoreCommand.class));
        commandManager.register(createInstance(DiscordCommand.class));
        commandManager.register(createInstance(NoobCommand.class));
        commandManager.register(createInstance(HideCommand.class));

        commandManager.register(createInstance(PlayCommand.class));
        commandManager.register(createInstance(WhereCommand.class));
        commandManager.register(createInstance(ListCommand.class));
        commandManager.register(createInstance(MsgCommand.class));
        commandManager.register(createInstance(ReplyCommand.class));

        commandManager.register(createInstance(RequestCommand.class));
        commandManager.register(createInstance(RejectCommand.class));
        commandManager.register(createInstance(InviteCommand.class));
        commandManager.register(createInstance(AcceptCommand.class));
        commandManager.register(createInstance(JoinCommand.class));

        commandManager.register(createInstance(MapCommand.class));

        commandManager.register(createInstance(SFindCommand.class));
        commandManager.register(createInstance(VanishCommand.class));
        commandManager.register(createInstance(UnvanishCommand.class));

        commandManager.register(createInstance(PHelpCommand.class));
        commandManager.register(createInstance(PStatusCommand.class));
        commandManager.register(createInstance(PHistoryCommand.class));

        commandManager.register(createInstance(BanCommand.class));
        commandManager.register(createInstance(UnbanCommand.class));
        commandManager.register(createInstance(MuteCommand.class));
        commandManager.register(createInstance(UnmuteCommand.class));
        commandManager.register(createInstance(KickCommand.class));
    }

    public @NotNull Collection<HealthCheck> readinessChecks() {
        //todo this should be probing session service for hubs, and session + maps for maps
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"),
                () -> {
                    var future = new CompletableFuture<>();
                    MinecraftServer.getSchedulerManager().scheduleNextTick(() -> future.complete(null));
                    try {
                        future.join();
                        return HealthCheckResponse.up("tick");
                    } catch (Exception e) {
                        return HealthCheckResponse.down("tick");
                    }
                },
                () -> isReady ? HealthCheckResponse.up("hub") : HealthCheckResponse.down("hub"),
                shutdowner
        );
    }

    public @NotNull Shutdowner shutdowner() {
        return shutdowner;
    }

    protected <T> void addBinding(@Nullable Class<T> type, @NotNull T instance, @NotNull String... names) {
        if (type != null) {
            injector.bind(type, instance);
            guiController.addBinding(type.getSimpleName().toLowerCase(Locale.ROOT), instance);
        }

        for (var name : names) {
            guiController.addBinding(name, instance);
        }
    }

    @Override
    public <T> @NotNull T createInstance(@NotNull Class<T> type) {
        return createInstance(type, null);
    }

    @Override
    public <T> @NotNull T createInstance(@NotNull Class<T> type, @Nullable Map<Class<?>, Object> context) {
        try {
            var injector = this.injector.injector();
            if (context != null && !context.isEmpty()) {
                injector = Injectors.child(injector, context);
            }

            return injector.getInstance(type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + type, e);
        }
    }

    @Override
    public void showView(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController.show(player, viewProvider);
    }

    /**
     * Returns a future which completes when the server has no more tasks to execute.
     *
     * <p>This is called when graceful shutdown has begun, and the server will be shutdown
     * either when this function completes, or when the timeout is reached.</p>
     *
     * <p>The future is not guaranteed to complete before other shutdown tasks are triggered (ie in the timeout case).</p>
     */
    private CompletableFuture<Void> awaitQuiescence() {
        Audiences.players().sendMessage(Component.text("Shutdown started (this message is temp)"));

        var connectionManager = MinecraftServer.getConnectionManager();
        if (connectionManager.getOnlinePlayers().isEmpty()) return CompletableFuture.completedFuture(null);

        // Wait for all players to disconnect
        var future = new CompletableFuture<Void>();
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            if (connectionManager.getOnlinePlayers().isEmpty())
                future.complete(null);
        });
        return future;
    }

    private @NotNull OpenTelemetry initTracing(@NotNull ConfigLoaderV3 config) {
        Resource resource = Resource.getDefault().toBuilder()
                .put(ResourceAttributes.SERVICE_NAME, name())
                .put(ResourceAttributes.SERVICE_VERSION, ServerRuntime.getRuntime().version())
                .build();

        var tracingConfig = config.get(TracingConfig.class);
        SpanExporter spanExporter;
        if (tracingConfig.otlpHttp() != null && !tracingConfig.otlpHttp().isEmpty()) {
            spanExporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(tracingConfig.otlpHttp())
                    .build();
        } else {
            spanExporter = LoggingSpanExporter.create();
        }

        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator
                        .composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                .buildAndRegisterGlobal();
    }

    protected @NotNull DebugCommand createDebugCommand() {
        return createInstance(DebugCommand.class);
    }

    public void handleUncaughtException(@NotNull Throwable t) {
        logger.error("An uncaught exception has been handled", t);
    }

    /**
     * Transfers the player session to this server and loads the required player data.
     */
    @Blocking
    protected boolean transferPlayerSession(@NotNull Player player, @NotNull Presence presence) {
        // Make the required requests in parallel. This is valid even though transferSession can fail, because
        // the other two requests (getting map player data, getting backpack) are idempotent/valid to do at
        // any point.
        var playerId = player.getUuid().toString();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var transferReq = new SessionTransferRequest(
                    presence.instanceId(), presence.type(),
                    presence.state(), presence.mapId()
            );
            var sessionResponseFuture = scope.fork(FutureUtil.wrap(() -> sessionService.transferSessionV2(playerId, transferReq)));
            var mapPlayerDataFuture = scope.fork(FutureUtil.wrap(() -> mapService.getMapPlayerData(playerId)));
            var backpackDataFuture = scope.fork(FutureUtil.wrap(() -> playerService.getPlayerBackpack(playerId)));

            scope.join();

            var sessionResponse = sessionResponseFuture.get();
            player.setTag(PlayerDataV2.TAG, sessionResponse.data());
            sessionManager.updateSessionOptimistic(sessionResponse.session(), new SessionStateUpdateRequest.Metadata());
            player.setTag(MapPlayerData.TAG, mapPlayerDataFuture.get());
            var backpack = new PlayerBackpack(player);
            player.setTag(PlayerBackpack.TAG, backpack);
            backpack.update(backpackDataFuture.get());

            // If the player is joining vanished, configure them that way.
            if (sessionResponse.session().hidden()) {
                logger.info("joining player {} is vanished", player.getUsername());
                sessionManager.configureVanishedPlayer(player);
            }

            return true;
        } catch (SessionService.UnauthorizedError ignored) {
            player.kick(Component.text("The server is currently in a closed beta.\nVisit ")
                    .append(Component.text("hollowcube.net").clickEvent(ClickEvent.openUrl("https://hollowcube.net/")))
                    .append(Component.text(" for more information.")));
            return false;
        } catch (Exception e) {
            logger.error("failed to create session", e);
            player.kick(Component.text("Failed to login. Please try again later."));
            return false;
        }
    }

    protected void handleFirstSpawn(@NotNull Player player) {
        logger.info("doing spawn for {}", player.getUsername());
        var playerData = PlayerDataV2.fromPlayer(player);

        // Player init
        player.setDisplayName(playerData.displayName2().build(DisplayName.Context.TAB_LIST));
        MiscFunctionality.assignTeam(player);
        Emoji.sendTabCompletions(player);
        MiscFunctionality.sendBetaHeader(player);

        PlayerBackpack.fromPlayer(player).refresh();

        var actionBar = ActionBar.forPlayer(player);
        actionBar.addProvider(MiscFunctionality::buildCurrencyDisplay);
        actionBar.addProvider(new ExpBarRenderer());

        // Add the player to the world they are spawning into
        //todo need to support joining as a spectator
        var world = MapWorld.unsafeFromInstance(player.getInstance());
        if (world == null) { // Sanity check
            player.kick("unknown error");
            return;
        }
        FutureUtil.submitVirtual(() -> {
            try {
                world.addPlayer(player);
                player.setAutoViewEntities(true); // See comment in AbstractMapWorld#configurePlayer
            } catch (Exception e) {
                MinecraftServer.getExceptionManager().handleException(e);
                player.kick(Component.text("Failed to join the world. Please try again later."));
            }
        });

        if (CoreFeatureFlags.SERVER_STAT_OVERLAY.test(player)) {
            actionBar.addProvider(new ServerStatsHud());
        }

        // Garbage below

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());
    }

    protected void handlePlayerDisconnect(@NotNull Player player) {
        logger.info("disconnect - {}", player.getUsername());
    }


}

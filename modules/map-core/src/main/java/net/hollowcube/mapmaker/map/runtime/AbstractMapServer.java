package net.hollowcube.mapmaker.map.runtime;

import com.google.gson.JsonObject;
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
import net.hollowcube.common.events.EventExtensions;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.chat.ChatAutoCompleter;
import net.hollowcube.mapmaker.chat.ChatChannelDisplay;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.chat.announcements.ChatAnnouncer;
import net.hollowcube.mapmaker.command.*;
import net.hollowcube.mapmaker.command.chat.ChannelCommand;
import net.hollowcube.mapmaker.command.chat.ChatCommand;
import net.hollowcube.mapmaker.command.chat.MsgCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.map.MapCommand;
import net.hollowcube.mapmaker.command.punish.*;
import net.hollowcube.mapmaker.command.staff.SFindCommand;
import net.hollowcube.mapmaker.command.staff.UnvanishCommand;
import net.hollowcube.mapmaker.command.staff.VanishCommand;
import net.hollowcube.mapmaker.command.store.HypercubeCommand;
import net.hollowcube.mapmaker.command.store.StoreCommand;
import net.hollowcube.mapmaker.command.util.*;
import net.hollowcube.mapmaker.config.*;
import net.hollowcube.mapmaker.consumer.PlayerDataUpdateConsumer;
import net.hollowcube.mapmaker.cosmetic.CosmeticInventoryHandler;
import net.hollowcube.mapmaker.cosmetic.impl.accessory.AbstractAccessoryImpl;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.posthog.PostHogFeatureFlagProvider;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.gui.settings.PlayerSettingsScreen;
import net.hollowcube.mapmaker.invite.MapInviteAcceptedOrRejectedListener;
import net.hollowcube.mapmaker.invite.MapInviteListener;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.invite.PlayerInviteServiceImpl;
import net.hollowcube.mapmaker.kafka.FriendlyProducer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.handler.BlockHandlers;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.util.AnonHealthCheck;
import net.hollowcube.mapmaker.map.util.DynamicController;
import net.hollowcube.mapmaker.misc.ExpBarRenderer;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.noop.*;
import net.hollowcube.mapmaker.obungus.ObungusService;
import net.hollowcube.mapmaker.obungus.ObungusServiceImpl;
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
import net.hollowcube.mapmaker.util.*;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.server.common.ServerLinksPacket;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractMapServer implements MapServer {
    private final Logger logger = LoggerFactory.getLogger(MapServer.class);

    protected final ConfigLoaderV3 config;
    protected final GlobalConfig globalConfig;

    protected final OpenTelemetry otel;
    private final SessionService sessionService;
    private final PlayerService playerService;
    private final MapService mapService;
    private final PermManager permManager;
    private final PunishmentService punishmentService;
    private PlayerInviteService inviteService; // So many dependencies very yikes

    // Listeners for other features
    private ServerBridge bridge;
    private FriendlyProducer producer;

    private SessionManager sessionManager;
    private ChatMessageListener chatMessageListener;
    private MapInviteListener mapInviteListener;
    private MapInviteAcceptedOrRejectedListener mapInviteAcceptedOrRejectedListener;
    private PlayerDataUpdateConsumer playerDataUpdateConsumer;

    private final CommandManager commandManager = new CommandManagerImpl();

    private DynamicController guiController = new DynamicController();
    private Map<Class<?>, Object> facets = new HashMap<>(); // Not concurrent, not editable after start.

    private volatile boolean isReady = false; // Corresponds to the associated health check
    private final Shutdowner shutdowner = new Shutdowner(this::awaitQuiescence);

    protected AbstractMapServer(@NotNull ConfigLoaderV3 config) {
        config.dump();

        this.config = config;
        this.globalConfig = config.get(GlobalConfig.class);

        this.otel = initTracing(config);

        var playerServiceUrl = config.get(Player_ServiceConfig.class).url();
        if (!playerServiceUrl.isEmpty()) {
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

        var sessionServiceUrl = config.get(Session_ServiceConfig.class).url();
        if (!sessionServiceUrl.isEmpty()) sessionService = new SessionServiceImpl(otel, sessionServiceUrl);
        else if (globalConfig.noop()) sessionService = new NoopSessionService();
        else sessionService = new SessionServiceImpl(otel, "http://localhost:9127"); // tilt

        var mapServiceUrl = config.get(Map_ServiceConfig.class).url();
        if (!mapServiceUrl.isEmpty()) mapService = new MapServiceImpl(mapServiceUrl);
        else if (globalConfig.noop()) mapService = new NoopMapService();
        else mapService = new MapServiceImpl("http://localhost:9125"); // tilt

        if (globalConfig.noop()) {
            permManager = new NoopPermManager();
        } else {
            var spicedbConfig = config.get(SpiceDBConfig.class);
            var spicedbUrl = spicedbConfig.url();
            if (spicedbUrl.isEmpty()) spicedbUrl = "http://localhost:8443";
            var spicedbToken = spicedbConfig.token();
            if (spicedbToken.isEmpty()) spicedbToken = "supersecretkey";
            permManager = new PermManagerImpl(otel, spicedbUrl, spicedbToken);
        }

        var obungusService = new ObungusServiceImpl(otel, "http://localhost:9125");
        addBinding(ObungusService.class, obungusService, "obungus", "obungusService");

        ShopUpgradeCache.init(permManager);
    }

    protected abstract @NotNull String name();

    @Override
    public <T> @NotNull T facet(@NotNull Class<T> type) {
        return type.cast(facets.get(type));
    }

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

        EventExtensions.init();

        // Block handlers are always necessary to prevent invisible/visually incorrect blocks,
        // even if we don't have block placement.
        BlockHandlers.init();

        // Dependent service init

        var unleashConfig = config.get(UnleashConfig.class);
        if (unleashConfig.usePosthog()) {
            logger.info("Posthog is enabled, loading feature flag provider");
            // project api key is not a secret.
            PostHog.init("phc_mK0jji1aC3hvMBGLOLjuVARqolDGPS9AiuNUOhMwVyA", config -> config
                    .personalApiKey(unleashConfig.posthogPersonalApiKey())
                    .endpoint("https://us.i.posthog.com")
                    .featureFlagsPollingInterval(Duration.ofMinutes(10))
                    .exceptionMiddleware(AbstractMapServer::posthogExceptionMiddleware));
            shutdowner.queue("posthog", PostHog::shutdown);

            FeatureFlagProvider.replaceGlobals(new PostHogFeatureFlagProvider());
        } else {
            FeatureFlagProvider.replaceGlobals((ignored1, ignored2) -> unleashConfig.defaultAction());
        }
        shutdowner.queue("feature-flag-provider", FeatureFlagProvider.current()::close);

        bridge = createBridge();

        var kafkaConfig = config.get(KafkaConfig.class);
        sessionManager = new SessionManager(sessionService(), playerService(), permManager(), kafkaConfig, globalConfig.noop());
        shutdowner.queue("session-manager", sessionManager::close);
        FutureUtil.submitVirtual(sessionManager()::sync); // Sync existing sessions with remote

        // Must be initialized this late because of all its dependencies. this is pretty yikes im not a big fan
        var inviteServiceUrl = System.getenv("MAPMAKER_PLAYER_INVITE_SERVICE_URL");
        if (inviteServiceUrl != null)
            this.inviteService = new PlayerInviteServiceImpl(otel, inviteServiceUrl, playerService, mapService, sessionManager, bridge, permManager);
        else if (globalConfig.noop()) this.inviteService = new NoopPlayerInviteService();
        else {
            this.inviteService = new PlayerInviteServiceImpl(otel, "http://localhost:9127", playerService, mapService, sessionManager, bridge, permManager); // tilt
        }

        // Create one producer to reuse.
        producer = new FriendlyProducer(kafkaConfig.bootstrapServers(), globalConfig.noop());
        facets.put(FriendlyProducer.class, producer);
        shutdowner.queue("kafka-producer", producer::close);

        if (!globalConfig.noop()) {
            mapInviteListener = new MapInviteListener(mapService, playerService, sessionManager, kafkaConfig.bootstrapServers());
            shutdowner.queue("map-invite-listener", mapInviteListener::close);

            mapInviteAcceptedOrRejectedListener = new MapInviteAcceptedOrRejectedListener(mapService, playerService, sessionManager, bridge(), kafkaConfig.bootstrapServers());
            shutdowner.queue("map-invite-acceptance-listener", mapInviteAcceptedOrRejectedListener::close);

            var punishmentCreatedListener = new PunishmentManagementListener(playerService, permManager, kafkaConfig.bootstrapServers());
            shutdowner.queue("punishment-listener", punishmentCreatedListener::close);

            chatMessageListener = new ChatMessageListener(sessionManager, playerService, mapService, punishmentService, kafkaConfig.bootstrapServers(), producer, permManager);
            facets.put(ChatMessageListener.class, chatMessageListener);
            shutdowner.queue("chat-message-listener", chatMessageListener::close);
            MinecraftServer.getPacketListenerManager().setPlayListener(ClientChatMessagePacket.class, chatMessageListener);

            playerDataUpdateConsumer = new PlayerDataUpdateConsumer(kafkaConfig.bootstrapServers(), playerService);
            shutdowner.queue("player-data-listener", playerDataUpdateConsumer::close);
        }

        ChatAnnouncer.setupAnnouncements(config, sessionManager(), shutdowner);

        facets.put(Controller.class, guiController);
        prepareStart();

        // Copy the facets map to prevent modification
        facets = Map.copyOf(facets);

        // Finally, mark the service as ready for Kubernetes
        isReady = true;
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

    @Override
    public @NotNull Scheduler scheduler() {
        return MinecraftServer.getSchedulerManager();
    }

    protected abstract @NotNull ServerBridge createBridge();

    /**
     * Called just before the server starts, but after all services have been initialized.
     */
    protected void prepareStart() {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        CompatProvider.load(globalEventHandler);

        CosmeticInventoryHandler.init(guiController);
        AbstractAccessoryImpl.addListeners(globalEventHandler);

        PlayerSettingsScreen.init(playerService(), globalEventHandler);

        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
        globalEventHandler.addChild(entityEvents);
        MapEntities.init(entityEvents);

        addBinding(MapServer.class, this, "mapServer", "server");
        addBinding(ConfigLoaderV3.class, config);

        addBinding(SessionService.class, sessionService, "sessionService");
        addBinding(PlayerService.class, playerService, "playerService");
        addBinding(MapService.class, mapService, "mapService");
        addBinding(PermManager.class, permManager, "permManager");
        addBinding(PunishmentService.class, punishmentService, "punishmentService");
        addBinding(PlayerInviteService.class, inviteService, "playerInviteService");

        addBinding(SessionManager.class, sessionManager, "sessionManager");
        addBinding(ServerBridge.class, bridge(), "bridge");

        boolean fullInstance = !globalConfig.noop();

        commandManager.register(new MinestomCommand());
        commandManager.register(new EmojisCommand());
        if (fullInstance) commandManager.register(new CosmeticsCommand(guiController()));
        if (fullInstance) commandManager.register(new RulesCommand());
        commandManager.register(createDebugCommand());
        commandManager.register(new StoreCommand(playerService(), permManager()));
        commandManager.register(new HypercubeCommand(playerService(), permManager()));
        commandManager.register(new DiscordCommand());
        if (fullInstance) commandManager.register(new LinkCommand(playerService()));
        if (fullInstance) commandManager.register(new TotpCommand(playerService(), guiController()));
        commandManager.register(new NoobCommand(permManager()));
        commandManager.register(new HideCommand(playerService()));

        if (fullInstance) {
            commandManager.register(new PlayCommand(playerService(), mapService(), sessionManager(), bridge()));
            commandManager.register(new WhereCommand(sessionManager(), playerService(), mapService(), permManager()));
            commandManager.register(new ListCommand(sessionManager(), playerService()));
            commandManager.register(new MsgCommand(sessionManager(), mapService(), chatMessageListener));
            commandManager.register(new ChannelCommand.Global(sessionManager(), mapService(), chatMessageListener));
            commandManager.register(new ChannelCommand.Local(sessionManager(), mapService(), chatMessageListener));
            commandManager.register(new ChannelCommand.Reply(sessionManager(), mapService(), chatMessageListener));
            commandManager.register(new ChannelCommand.Staff(sessionManager(), mapService(), chatMessageListener, permManager()));
            commandManager.register(new ChatCommand(playerService(), permManager()));
        }

        if (fullInstance) {
            commandManager.register(new RequestCommand(inviteService(), playerService(), sessionManager()));
            commandManager.register(new RejectCommand(inviteService(), playerService(), sessionManager()));
            commandManager.register(new InviteCommand(inviteService(), playerService(), sessionManager()));
            commandManager.register(new AcceptCommand(inviteService(), playerService(), sessionManager()));
            commandManager.register(new JoinCommand(inviteService(), playerService(), sessionManager()));
        }

        commandManager.register(new MapCommand(guiController(), playerService(), mapService(), permManager(), bridge(), producer));

        if (fullInstance) {
            commandManager.register(new SFindCommand(mapService(), playerService(), sessionManager(), permManager()));
            commandManager.register(new VanishCommand(sessionManager(), playerService(), permManager()));
            commandManager.register(new UnvanishCommand(sessionManager(), playerService(), permManager()));
        }

        if (fullInstance) {
            commandManager.register(new PHelpCommand(punishmentService(), permManager()));
            commandManager.register(new PStatusCommand(playerService(), punishmentService(), permManager()));
            commandManager.register(new PHistoryCommand(playerService(), punishmentService(), permManager()));
        }

        if (fullInstance) {
            FutureUtil.submitVirtual(() -> commandManager.register(new BanCommand(punishmentService(), playerService(), permManager())));
            commandManager.register(new UnbanCommand(punishmentService(), playerService(), permManager()));
            FutureUtil.submitVirtual(() -> commandManager.register(new MuteCommand(punishmentService(), playerService(), permManager())));
            commandManager.register(new UnmuteCommand(punishmentService(), playerService(), permManager()));
            commandManager.register(new KickCommand(punishmentService(), sessionManager(), permManager()));
        }

        DataFixer.buildModel();
    }

    public @NotNull List<HttpServerWrapper.HealthCheck> healthChecks() {
        return List.of(
                shutdowner(),
                new AnonHealthCheck("minestom", MinecraftServer::isStarted),
                new AnonHealthCheck("hub", () -> isReady),
                new AnonHealthCheck("tick", () -> {
                    var future = new CompletableFuture<Boolean>();
                    MinecraftServer.getSchedulerManager().scheduleNextTick(() -> future.complete(true));
                    return FutureUtil.getUnchecked(future);
                })
        );
    }

    public @NotNull Shutdowner shutdowner() {
        return shutdowner;
    }

    protected <T> void addBinding(@Nullable Class<T> type, @NotNull T instance, @NotNull String... names) {
        if (!type.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("Instance " + instance + " is not of type " + type);
        }

        if (type != null) {
            facets.put(type, instance);
            guiController.addBinding(type.getSimpleName().toLowerCase(Locale.ROOT), instance);
        }

        for (var name : names) {
            guiController.addBinding(name, instance);
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
        EventDispatcher.call(new ServerBeginShutdownEvent());

        var connectionManager = MinecraftServer.getConnectionManager();
        if (connectionManager.getOnlinePlayers().isEmpty()) return CompletableFuture.completedFuture(null);

        if (ServerRuntime.getRuntime().isDevelopment()) {
            for (var player : List.copyOf(connectionManager.getOnlinePlayers())) {
                player.kick(Component.text("Dev server shutdown"));
            }
            return CompletableFuture.completedFuture(null);
        }

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
        if (tracingConfig.otlpEndpoint() != null && !tracingConfig.otlpEndpoint().isEmpty()) {
            spanExporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(tracingConfig.otlpEndpoint())
                    .build();
        } else if (tracingConfig.noop()) {
            spanExporter = NoopSpanExporter.INSTANCE;
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
        return new DebugCommand(playerService(), permManager(), mapService());
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
        try {
            var transferReq = new SessionTransferRequest(
                    presence.instanceId(), presence.type(),
                    presence.state(), presence.mapId()
            );
            var sessionResponseFuture = FutureUtil.fork(() -> sessionService.transferSession(playerId, transferReq));
            var mapPlayerDataFuture = FutureUtil.fork(() -> mapService.getMapPlayerData(playerId));
            var backpackDataFuture = FutureUtil.fork(() -> playerService.getPlayerBackpack(playerId));

            CompletableFuture.allOf(sessionResponseFuture, mapPlayerDataFuture, backpackDataFuture).join();

            var sessionResponse = sessionResponseFuture.get();
            player.setTag(CompatProvider.FIRST_JOIN_TAG, sessionResponse.isJoin());
            player.setTag(PlayerData.TAG, sessionResponse.data());
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
        var playerData = PlayerData.fromPlayer(player);

        player.sendPacket(new ServerLinksPacket(List.of(
                new ServerLinksPacket.Entry(ServerLinksPacket.KnownLinkType.WEBSITE, "https://hollowcube.net/"),
                new ServerLinksPacket.Entry(Component.text("Store"), "https://hollowcube.net/store"),
                new ServerLinksPacket.Entry(ServerLinksPacket.KnownLinkType.NEWS, "https://hollowcube.net/news"),
                new ServerLinksPacket.Entry(ServerLinksPacket.KnownLinkType.COMMUNITY_GUIDELINES, "https://hollowcube.net/rules"),
                new ServerLinksPacket.Entry(ServerLinksPacket.KnownLinkType.SUPPORT, "https://hollowcube.net/contact"),
                new ServerLinksPacket.Entry(ServerLinksPacket.KnownLinkType.BUG_REPORT, "https://discord.hollowcube.net/")
        )));

        // Player init
        player.setDisplayName(playerData.displayName2().build(DisplayName.Context.DEFAULT));
        MiscFunctionality.assignTeam(player);
        ChatAutoCompleter.sendSuggestions(player);

        PlayerBackpack.fromPlayer(player).refresh();

        var actionBar = ActionBar.forPlayer(player);
        actionBar.addProvider(ChatChannelDisplay.INSTANCE);
        actionBar.addProvider(MiscFunctionality::buildCurrencyDisplay);
        actionBar.addProvider(new ExpBarRenderer());

        // Add the player to the world they are spawning into
        var world = MapWorld.forInstance(player.getInstance());
        if (world == null) { // Sanity check
            player.kick("unknown error");
            return;
        }
        try {
            world.spawnPlayer(player);

            // See comment in AbstractMapWorld#configurePlayer
            player.setAutoViewEntities(true);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.kick(Component.text("Failed to join the world. Please try again later."));
        }

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

    private static final Pattern MINESTOM_PACKET_EXCEPTION;

    static {
        try {
            MINESTOM_PACKET_EXCEPTION = Pattern.compile("Packet id .+ isn't registered!");
        } catch (PatternSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean posthogExceptionMiddleware(@NotNull Throwable t, JsonObject message) {
        var oom = t;
        while (oom != null) {
            if (oom instanceof OutOfMemoryError) return false;
            else oom = oom.getCause();
        }

        if (MINESTOM_PACKET_EXCEPTION.matcher(t.toString()).find())
            return false;
        // Some expected IO exceptions that we don't want to report.
        if (t.toString().contains("header parser received no bytes") || t.toString().contains("Connection reset by peer"))
            return false;

        // Drop exceptions where the jvm has removed the stacktrace after the nth occurrance
        if (t.getStackTrace() == null || t.getStackTrace().length == 0)
            return false;

        // todo fancier exception grouping
        return true;
    }

}

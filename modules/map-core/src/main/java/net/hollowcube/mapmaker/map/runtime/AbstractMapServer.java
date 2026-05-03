package net.hollowcube.mapmaker.map.runtime;

import com.google.gson.JsonObject;
import io.nats.client.Nats;
import io.nats.client.Options;
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
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.dialogs.DialogButtons;
import net.hollowcube.common.events.EventExtensions;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.HttpClientWrapper;
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
import net.hollowcube.mapmaker.command.relationship.BlockCommand;
import net.hollowcube.mapmaker.command.relationship.UnblockCommand;
import net.hollowcube.mapmaker.command.relationship.friend.FriendCommand;
import net.hollowcube.mapmaker.command.staff.SFindCommand;
import net.hollowcube.mapmaker.command.staff.StaffCommand;
import net.hollowcube.mapmaker.command.staff.UnvanishCommand;
import net.hollowcube.mapmaker.command.staff.VanishCommand;
import net.hollowcube.mapmaker.command.store.HypercubeCommand;
import net.hollowcube.mapmaker.command.store.StoreCommand;
import net.hollowcube.mapmaker.command.util.*;
import net.hollowcube.mapmaker.config.*;
import net.hollowcube.mapmaker.consumer.PlayerDataUpdateConsumer;
import net.hollowcube.mapmaker.cosmetic.CosmeticEventHandler;
import net.hollowcube.mapmaker.cosmetic.impl.accessory.AbstractAccessoryImpl;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.posthog.PostHogFeatureFlagProvider;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.gui.settings.PlayerSettingsScreen;
import net.hollowcube.mapmaker.invite.MapInviteAcceptedOrRejectedListener;
import net.hollowcube.mapmaker.invite.MapInviteListener;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.invite.PlayerInviteServiceImpl;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapServiceImpl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.handler.BlockHandlers;
import net.hollowcube.mapmaker.map.command.BugReportCommand;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.util.ACHook;
import net.hollowcube.mapmaker.map.util.AnonHealthCheck;
import net.hollowcube.mapmaker.map.util.ServerStatsHud;
import net.hollowcube.mapmaker.misc.ExpBarRenderer;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.noop.*;
import net.hollowcube.mapmaker.notifications.NotificationsConsumer;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.punishments.PunishmentManagementListener;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.punishments.PunishmentServiceImpl;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.session.SessionStateUpdateRequest;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.*;
import net.hollowcube.mapmaker.util.nats.JetStreamWrapper;
import net.hollowcube.mapmaker.util.nats.NatsConfig;
import net.hollowcube.mapmaker.util.telemetry.NoopSpanExporter;
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
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.server.common.ServerLinksPacket;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractMapServer implements MapServer {
    private final Logger logger = LoggerFactory.getLogger(MapServer.class);

    protected final ConfigLoaderV3 config;
    protected final GlobalConfig globalConfig;

    private final ACHook acHook;

    protected final OpenTelemetry otel;
    private final ApiClient api;
    private final SessionService sessionService;
    private final PlayerService playerService;
    private final MapService mapService;
    private final PunishmentService punishmentService;
    private PlayerInviteService inviteService; // So many dependencies very yikes

    // Listeners for other features
    private ServerBridge bridge;
    protected JetStreamWrapper jetStream;

    private SessionManager sessionManager;
    private ChatMessageListener chatMessageListener;
    private MapInviteListener mapInviteListener;
    private MapInviteAcceptedOrRejectedListener mapInviteAcceptedOrRejectedListener;
    private PlayerDataUpdateConsumer playerDataUpdateConsumer;

    private final CommandManager commandManager = new CommandManagerImpl();

    private Map<Class<?>, Object> facets = new HashMap<>(); // Not concurrent, not editable after start.

    private volatile boolean isReady = false; // Corresponds to the associated health check
    private final Shutdowner shutdowner = new Shutdowner(this::awaitQuiescence);

    private final List<String> remoteCommandNames = new ArrayList<>();

    protected AbstractMapServer(@NotNull ConfigLoaderV3 config) {
        this.config = config;
        this.globalConfig = config.get(GlobalConfig.class);

        this.otel = initTracing(config);

        var apiUrl = config.get(Player_ServiceConfig.class).url();
        if (apiUrl.isEmpty()) apiUrl = "http://localhost:9127";
        var http = new HttpClientWrapper(otel, apiUrl);
        this.api = new ApiClient(http);

        var playerServiceUrl = config.get(Player_ServiceConfig.class).url();
        if (!playerServiceUrl.isEmpty()) {
            playerService = new PlayerServiceImpl(otel, playerServiceUrl);
            punishmentService = new PunishmentServiceImpl(playerServiceUrl);
        } else if (globalConfig.noop()) {
            playerService = new NoopPlayerService();
            punishmentService = new NoopPunishmentService();
        } else {
            var localUrl = "http://localhost:9127"; // tilt
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
        else mapService = new MapServiceImpl("http://localhost:9127"); // tilt

        this.acHook = ServiceLoader.load(ACHook.class).findFirst().orElse(null);
    }

    protected abstract @NotNull String name();

    @Override
    public <T> @NotNull T facet(@NotNull Class<T> type) {
        return type.cast(facets.get(type));
    }

    @Blocking
    public final void start() {
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
                .blockUntilLocalFlagsLoaded(Duration.ofSeconds(3)) // if it fails, it'll still get re-fetched, just it'll return DISABLED for initially joining players
                .endpoint("http://api-server.mapmaker:9124/posthog")
                .featureFlagsPollingInterval(Duration.ofMinutes(10))
                .allowRemoteFeatureFlagEvaluation(false)
                .blockUntilLocalFlagsLoaded(Duration.ofSeconds(10))
                .exceptionMiddleware(AbstractMapServer::posthogExceptionMiddleware)
                .gson(AbstractHttpService.GSON));
            shutdowner.queue("posthog", PostHog::shutdown);

            FeatureFlagProvider.replaceGlobals(new PostHogFeatureFlagProvider());
        } else {
            FeatureFlagProvider.replaceGlobals((ignored1, ignored2) -> unleashConfig.defaultAction());
        }
        shutdowner.queue("feature-flag-provider", FeatureFlagProvider.current()::close);

        bridge = createBridge();

        try {
            var nc = Nats.connect(Options.builder()
                .servers(config.get(NatsConfig.class).servers().split(","))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build());
            shutdowner.queue("nats", nc::close);
            jetStream = new JetStreamWrapper(nc, otel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        sessionManager = new SessionManager(sessionService(), playerService(), api().players, jetStream);
        shutdowner.queue("session-manager", sessionManager::close);
        FutureUtil.submitVirtual(sessionManager()::sync); // Sync existing sessions with remote

        var transferConsumer = new PlayerTransferConsumer(bridge, jetStream);
        shutdowner.queue("player-transfer-consumer", transferConsumer::close);

        // Must be initialized this late because of all its dependencies. this is pretty yikes im not a big fan
        var inviteServiceUrl = System.getenv("MAPMAKER_PLAYER_INVITE_SERVICE_URL");
        if (inviteServiceUrl != null)
            this.inviteService = new PlayerInviteServiceImpl(otel, inviteServiceUrl, api, sessionManager, bridge);
        else if (globalConfig.noop()) this.inviteService = new NoopPlayerInviteService();
        else {
            this.inviteService = new PlayerInviteServiceImpl(otel, "http://localhost:9127", api, sessionManager, bridge); // tilt
        }

        var services = new ServiceContext(api(), playerService(), bridge());

        if (!globalConfig.noop()) {
            mapInviteListener = new MapInviteListener(api, sessionManager, jetStream);
            shutdowner.queue("map-invite-listener", mapInviteListener::close);

            mapInviteAcceptedOrRejectedListener = new MapInviteAcceptedOrRejectedListener(api, sessionManager, bridge(), jetStream);
            shutdowner.queue("map-invite-acceptance-listener", mapInviteAcceptedOrRejectedListener::close);

            var punishmentCreatedListener = new PunishmentManagementListener(api.players, jetStream);
            shutdowner.queue("punishment-listener", punishmentCreatedListener::close);

            chatMessageListener = new ChatMessageListener(sessionManager, api, punishmentService, jetStream);
            facets.put(ChatMessageListener.class, chatMessageListener);
            shutdowner.queue("chat-message-listener", chatMessageListener::close);
            MinecraftServer.getPacketListenerManager().setPlayListener(ClientChatMessagePacket.class, chatMessageListener);

            playerDataUpdateConsumer = new PlayerDataUpdateConsumer(api.players, jetStream);
            shutdowner.queue("player-data-listener", playerDataUpdateConsumer::close);

            var notificationsConsumer = new NotificationsConsumer(services, jetStream);
            shutdowner.queue("notifications-consumer", notificationsConsumer::close);
        }

        ChatAnnouncer.setupAnnouncements(config, sessionManager(), shutdowner);

        prepareStart();

        // Copy the facets map to prevent modification
        facets = Map.copyOf(facets);

        if (acHook != null) acHook.preReady(this);

        // Finally, mark the service as ready for Kubernetes
        isReady = true;
    }

    @Override
    public @NotNull ApiClient api() {
        return api;
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

    @Override
    public @NotNull CommandManager commandManager() {
        return commandManager;
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

        CosmeticEventHandler.init(playerService());
        AbstractAccessoryImpl.addListeners(globalEventHandler);

        PlayerSettingsScreen.init(playerService(), globalEventHandler);
        DialogButtons.init(globalEventHandler);

        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
        globalEventHandler.addChild(entityEvents);
        MapEntities.init(entityEvents);

        addBinding(MapServer.class, this, "mapServer", "server");
        addBinding(ConfigLoaderV3.class, config);

        addBinding(ApiClient.class, api, "api");
        addBinding(SessionService.class, sessionService, "sessionService");
        addBinding(PlayerService.class, playerService, "playerService");
        addBinding(MapService.class, mapService, "mapService");
        addBinding(PunishmentService.class, punishmentService, "punishmentService");
        addBinding(PlayerInviteService.class, inviteService, "playerInviteService");

        addBinding(SessionManager.class, sessionManager, "sessionManager");
        addBinding(ServerBridge.class, bridge(), "bridge");

        boolean fullInstance = !globalConfig.noop();

        commandManager.register(new MinestomCommand());
        commandManager.register(new EmojisCommand());
        if (fullInstance) commandManager.register(new CosmeticsCommand(playerService()));
        if (fullInstance) commandManager.register(new RulesCommand());
        commandManager.register(createDebugCommand());
        commandManager.register(new StoreCommand(playerService()));
        commandManager.register(new HypercubeCommand(api(), playerService()));
        commandManager.register(new DiscordCommand());
        if (fullInstance) commandManager.register(new TotpCommand(playerService()));
        commandManager.register(new NoobCommand());
        commandManager.register(new HideCommand(playerService()));
        commandManager.register(new SettingsCommand());
        commandManager.register(new BugReportCommand());
        commandManager.register(new UwUCommand(playerService()));

        if (fullInstance) {
            commandManager.register(new UnblockCommand(api().players, playerService()));
            commandManager.register(new BlockCommand(api().players, playerService()));
            commandManager.register(new FriendCommand(api(), playerService(), mapService(), sessionManager()));
        }

        if (fullInstance) {
            commandManager.register(new PlayCommand(api(), mapService(), sessionManager(), bridge()));
            commandManager.register(new WhereCommand(api(), sessionManager()));
            commandManager.register(new ListCommand(sessionManager(), api().players));
            commandManager.register(new MsgCommand(sessionManager(), api().maps, chatMessageListener, playerService()));
            commandManager.register(new ChannelCommand.Global(sessionManager(), api().maps, chatMessageListener));
            commandManager.register(new ChannelCommand.Local(sessionManager(), api().maps, chatMessageListener));
            commandManager.register(new ChannelCommand.Reply(sessionManager(), api().maps, chatMessageListener));
            commandManager.register(new ChannelCommand.Staff(sessionManager(), api().maps, chatMessageListener));
            commandManager.register(new ChatCommand(playerService()));
        }

        if (fullInstance) {
            commandManager.register(new RequestCommand(inviteService(), playerService(), api().players, sessionManager()));
            commandManager.register(new RejectCommand(inviteService(), playerService(), api().players, sessionManager()));
            commandManager.register(new InviteCommand(inviteService(), playerService(), api().players, sessionManager()));
            commandManager.register(new AcceptCommand(inviteService(), playerService(), api().players, sessionManager()));
            commandManager.register(new JoinCommand(inviteService(), playerService(), api().players, sessionManager()));
        }

        commandManager.register(new MapCommand(api(), playerService(), mapService(), bridge(), jetStream));

        if (fullInstance) {
            commandManager.register(new SFindCommand(api(), sessionManager()));
            commandManager.register(new VanishCommand(sessionManager(), playerService()));
            commandManager.register(new UnvanishCommand(sessionManager(), playerService()));
            commandManager.register(new StaffCommand(playerService()));
        }

        if (fullInstance) {
            commandManager.register(new PHelpCommand(punishmentService()));
            commandManager.register(new PStatusCommand(api().players, punishmentService()));
            commandManager.register(new PHistoryCommand(api().players, punishmentService()));
        }

        if (fullInstance) {
            commandManager.register(new UnbanCommand(punishmentService(), api().players));
            FutureUtil.submitVirtual(() -> commandManager.register(new MuteCommand(punishmentService(), api().players)));
            commandManager.register(new UnmuteCommand(punishmentService(), api()));
            commandManager.register(new KickCommand(punishmentService(), sessionManager()));
        }

        if (fullInstance) {
            loadRemoteCommands();
        }

        DataFixer.buildModel();
    }

    private void loadRemoteCommands() {
        for (var commandName : remoteCommandNames)
            commandManager().unregister(commandName);
        remoteCommandNames.clear();

        var interactions = api().interactions.getCommands();
        for (var interaction : interactions) {
            var cmd = new RemoteCommand(api(), interaction);
            remoteCommandNames.add(cmd.name());
            commandManager.register(cmd);
        }
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
        }
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
        var cmd = new DebugCommand(api(), bridge());
        cmd.createPermissionedSubcommand("reload-commands", (player, _) -> {
            loadRemoteCommands();
            player.sendMessage(Component.text("Reloaded commands"));
        }, "reload remote commands");
        return cmd;
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
            var session = sessionService.transferSession(playerId, transferReq);

            player.setTag(CompatProvider.FIRST_JOIN_TAG, session.isJoin());
            player.setTag(PlayerData.TAG, session.data());
            sessionManager.updateSessionOptimistic(session.session(), new SessionStateUpdateRequest.Metadata());
            var backpack = new PlayerBackpack(player);
            player.setTag(PlayerBackpack.TAG, backpack);

            // If the player is joining vanished, configure them that way.
            if (session.session().hidden()) {
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
        actionBar.addProvider(MiscFunctionality.CurrencyDisplayHud.INSTANCE);
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

        if (Boolean.TRUE.equals(player.getTag(CompatProvider.FIRST_JOIN_TAG))) {
            var now = LocalDateTime.now(ZoneOffset.ofHours(-5));
            if (now.getDayOfMonth() == 1 && now.getMonthValue() == 4) {
                player.sendMessage(Component.translatable("join.uwu"));
            }
        }
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

        var runtime = ServerRuntime.getRuntime();
        message.addProperty("$app_version", runtime.version());
        message.addProperty("server", runtime.hostname());

        return true;
    }

}

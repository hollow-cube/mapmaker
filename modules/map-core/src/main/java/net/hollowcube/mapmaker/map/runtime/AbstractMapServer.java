package net.hollowcube.mapmaker.map.runtime;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.CommandManagerImpl;
import net.hollowcube.command.util.CommandHandlingPlayer;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.Injectors;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.chat.announcements.ChatAnnouncer;
import net.hollowcube.mapmaker.command.EmojisCommand;
import net.hollowcube.mapmaker.command.MapCommand;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.store.StoreCommand;
import net.hollowcube.mapmaker.command.util.DebugCommand;
import net.hollowcube.mapmaker.command.util.ListCommand;
import net.hollowcube.mapmaker.command.util.PingCommand;
import net.hollowcube.mapmaker.command.util.WhereCommand;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.config.GlobalConfig;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.feature.unleash.UnleashFeatureFlagProvider;
import net.hollowcube.mapmaker.invite.MapInviteAcceptedOrRejectedListener;
import net.hollowcube.mapmaker.invite.MapInviteListener;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.util.DynamicController;
import net.hollowcube.mapmaker.map.util.DynamicInjector;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.misc.noop.*;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PermManagerImpl;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AbstractMapServer implements MapServer {
    private final Logger logger = LoggerFactory.getLogger(MapServer.class);

    protected final ConfigLoaderV3 config;
    protected final GlobalConfig globalConfig;

    private final SessionService sessionService;
    private final PlayerService playerService;
    private final MapService mapService;
    private final PermManager permManager;
    private final PlayerInviteService inviteService;

    // Listeners for other features
    private MapAllocator allocator;
    private ServerBridge bridge;

    private SessionManager sessionManager;
    private ChatMessageListener chatMessageListener;
    private MapInviteListener mapInviteListener;
    private MapInviteAcceptedOrRejectedListener mapInviteAcceptedOrRejectedListener;

    private final CommandManager commandManager = new CommandManagerImpl();

    private DynamicController guiController = new DynamicController();
    private DynamicInjector injector = new DynamicInjector();

    private volatile boolean isReady = false; // Corresponds to the associated health check
    private final Shutdowner shutdowner = new Shutdowner(this::awaitQuiescence);

    protected AbstractMapServer(@NotNull ConfigLoaderV3 config) {
        this.config = config;
        this.globalConfig = config.get(GlobalConfig.class);

        var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
        if (playerServiceUrl != null) playerService = new PlayerServiceImpl(playerServiceUrl);
        else if (globalConfig.noop()) playerService = new NoopPlayerService();
        else playerService = new PlayerServiceImpl("http://localhost:9126"); // tilt

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

        var inviteServiceUrl = System.getenv("MAPMAKER_PLAYER_INVITE_SERVICE_URL");
        if (inviteServiceUrl != null) this.inviteService = new NoopPlayerInviteService();
//        if (inviteServiceUrl != null) this.inviteService = new PlayerInviteServiceImpl(inviteServiceUrl, this);
        else if (globalConfig.noop()) this.inviteService = new NoopPlayerInviteService();
        else this.inviteService = new NoopPlayerInviteService(); // tilt
//        else this.inviteService = new PlayerInviteServiceImpl("http://localhost:9127", this); // tilt
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
        MinecraftServer.getConnectionManager().setPlayerProvider(CommandHandlingPlayer.createDefaultProvider(commandManager));

        // Dependent service init

        var unleashConfig = config.get(UnleashConfig.class);
        if (unleashConfig.enabled()) {
            logger.info("Unleash is enabled, loading feature flag provider");
            var provider = new UnleashFeatureFlagProvider(unleashConfig);
            FeatureFlagProvider.replaceGlobals(provider);
        }

        allocator = createAllocator();
        shutdowner.queue(allocator::close);
        bridge = createBridge();

        var kafkaConfig = config.get(KafkaConfig.class);
        sessionManager = new SessionManager(sessionService, playerService, kafkaConfig, globalConfig.noop());
        shutdowner.queue(sessionManager::close);
        sessionManager().sync(); // Sync existing sessions with remote

        if (!globalConfig.noop()) {
            mapInviteListener = new MapInviteListener(mapService, playerService, sessionManager, kafkaConfig.bootstrapServersStr());
            shutdowner.queue(mapInviteListener::close);

            mapInviteAcceptedOrRejectedListener = new MapInviteAcceptedOrRejectedListener(mapService, playerService, sessionManager, bridge(), kafkaConfig.bootstrapServersStr());
            shutdowner.queue(mapInviteAcceptedOrRejectedListener::close);

            chatMessageListener = new ChatMessageListener(playerService, mapService, kafkaConfig.bootstrapServersStr());
            shutdowner.queue(chatMessageListener::close);
            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            packetListenerManager.setPlayListener(ClientChatMessagePacket.class, chatMessageListener);
        }

        ChatAnnouncer.setupAnnouncements(config, sessionManager());

        injector.bind(Controller.class, guiController);
        prepareStart();

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

    protected abstract @NotNull MapAllocator createAllocator();
    protected abstract @NotNull ServerBridge createBridge();

    /**
     * Called just before the server starts, but after all services have been initialized.
     */
    protected void prepareStart() {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
        globalEventHandler.addChild(entityEvents);
        MapEntities.init(entityEvents);

        addBinding(MapServer.class, this, "mapServer", "server");
        addBinding(ConfigLoaderV3.class, config);

        addBinding(SessionService.class, sessionService, "sessionService");
        addBinding(PlayerService.class, playerService, "playerService");
        addBinding(MapService.class, mapService, "mapService");
        addBinding(PermManager.class, permManager, "permManager");
        addBinding(PlayerInviteService.class, inviteService, "playerInviteService");

        addBinding(MapAllocator.class, allocator, "allocator");
        addBinding(SessionManager.class, sessionManager, "sessionManager");
        addBinding(ServerBridge.class, bridge(), "bridge");

        commandManager.register(createInstance(EmojisCommand.class));
        commandManager.register(createDebugCommand());
        commandManager.register(createInstance(PingCommand.class));
        commandManager.register(createInstance(StoreCommand.class));

        commandManager.register(createInstance(PlayCommand.class));
        commandManager.register(createInstance(WhereCommand.class));
        commandManager.register(createInstance(ListCommand.class));

        commandManager.register(createInstance(RequestCommand.class));
        commandManager.register(createInstance(RejectCommand.class));
        commandManager.register(createInstance(InviteCommand.class));
        commandManager.register(createInstance(AcceptCommand.class));
        commandManager.register(createInstance(JoinCommand.class));

        commandManager.register(createInstance(MapCommand.class));
    }

    public @NotNull Collection<HealthCheck> readinessChecks() {
        //todo this should be probing session service for hubs, and session + maps for maps
        return List.of(
                () -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"),
                () -> isReady ? HealthCheckResponse.up("hub") : HealthCheckResponse.down("hub"),
                shutdowner
        );
    }

    public @NotNull Shutdowner shutdowner() {
        return shutdowner;
    }

    protected <T> void addBinding(@Nullable Class<T> type, @NotNull T instance, @NotNull String... names) {
        if (type != null) injector.bind(type, instance);
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

        //todo
        return CompletableFuture.runAsync(() -> {
        }, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
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
                    presence.mapId(), presence.state()
            );
            var playerDataFuture = scope.fork(() -> sessionService.transferSessionV2(playerId, transferReq));
            var mapPlayerDataFuture = scope.fork(() -> mapService.getMapPlayerData(playerId));
            var backpackDataFuture = scope.fork(() -> playerService.getPlayerBackpack(playerId));

            scope.join();

            player.setTag(PlayerDataV2.TAG, playerDataFuture.get());
            player.setTag(MapPlayerData.TAG, mapPlayerDataFuture.get());
            var backpack = new PlayerBackpack(player);
            player.setTag(PlayerBackpack.TAG, backpack);
            backpack.update(backpackDataFuture.get());

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
        actionBar.addProvider(MiscFunctionality::buildExperienceBar);

        // Add the player to the world they are spawning into
        //todo need to support joining as a spectator
        var world = MapWorld.unsafeFromInstance(player.getInstance());
        if (world == null) { // Sanity check
            player.kick("unknown error");
            return;
        }
        world.addPlayer(player);

        // Garbage below

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());
    }

    protected void handlePlayerDisconnect(@NotNull Player player) {
        logger.info("disconnect - {}", player.getUsername());
    }


}

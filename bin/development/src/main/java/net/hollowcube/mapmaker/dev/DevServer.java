package net.hollowcube.mapmaker.dev;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import io.helidon.health.HealthSupport;
import io.helidon.metrics.prometheus.PrometheusSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.command.CommandManager;
import net.hollowcube.command.HelpCommand;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.config.Config;
import net.hollowcube.mapmaker.config.NewConfigProvider;
import net.hollowcube.mapmaker.config.VelocityConfig;
import net.hollowcube.mapmaker.config.http.HttpConfig;
import net.hollowcube.mapmaker.dev.chat.ChatMessageListener;
import net.hollowcube.mapmaker.dev.command.CommandRewriter;
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.command.EmojisCommand;
import net.hollowcube.mapmaker.dev.command.map.MapWorldCommand;
import net.hollowcube.mapmaker.dev.runtime.DevRuntime;
import net.hollowcube.mapmaker.dev.unleash.MapIdStrategy;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.misc.Emoji;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PermManagerImpl;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.hollowcube.mapmaker.world.KindaBadThingToFix;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.client.common.ClientResourcePackStatusPacket;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.resourcepack.ResourcePack;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public class DevServer {
    private static final Logger logger = LoggerFactory.getLogger(DevServer.class);

    private static final String RESOURCE_PACK_URL = "https://pub-620a83127bac451cbe2c402881b1b7d8.r2.dev/mapmaker-%s.zip";
    private static final UUID RESOURCE_PACK_ID = UUID.fromString("aceb326f-da15-45bc-bf2f-11940c21780c");
    private static final Tag<CompletableFuture<Void>> WAITING_FOR_RP_TAG = Tag.Transient("waiting_for_rp");

    public static Unleash UNLEASH_INSTANCE = null;

    private HubToMapBridge hubToMapBridge;

    public static void main(String[] args) {
        long start = System.nanoTime();

        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.command.async-virtual", "true");
        System.setProperty("minestom.event.multiple-parents", "true");
        System.setProperty("minestom.use-new-chunk-sending", "true");
        System.setProperty("minestom.experiment.pose-updates", "true");

        System.setProperty("minestom.new-chunk-sending-count-per-interval", "50");
        System.setProperty("minestom.new-chunk-sending-send-interval", "1");

        // Convert JUL messages to SLF4J
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Setup pyroscope profiler
        var pyroscopeEndpoint = System.getenv("MAPMAKER_PYROSCOPE_ENDPOINT");
        if (pyroscopeEndpoint != null) {
            logger.info("Enabling pyroscope profiling...");
            PyroscopeAgent.start(new io.pyroscope.javaagent.config.Config.Builder().setApplicationName("mapmaker").setProfilingEvent(EventType.ITIMER).setFormat(Format.JFR).setServerAddress(pyroscopeEndpoint).build());
        } else {
            logger.info("Skipping profiler...");
        }

        // Prometheus JVM exporters
        DefaultExports.initialize();

        // Load config
        Path configPath = Path.of("config.yaml");
        var config = Config.loadFromFile(configPath);
        var configProvider = NewConfigProvider.loadFromFile(configPath);

        // Begin server initialization
        var minecraftServer = MinecraftServer.init();
        MinecraftServer.getExceptionManager().setExceptionHandler(t -> logger.error("An uncaught exception has been handled", t));
        var server = new DevServer();

        // Add health check & metrics web server.
        var httpConfig = configProvider.get(HttpConfig.class);
        WebServer webServer = WebServer.builder().host(httpConfig.host()).port(httpConfig.port()).addRouting(Routing.builder().register(HealthSupport.builder().webContext("alive").addLiveness(() -> HealthCheckResponse.up("mapmaker")).build()).register(HealthSupport.builder().webContext("ready").addReadiness(server.readinessChecks()).build()).register(PrometheusSupport.create()).build()).build();
        webServer.start().thenAccept(ws -> logger.info("Web server is running at {}:{}", config.http().host(), ws.port()));

        // Finish server initialization
        server.start(config, configProvider);
        minecraftServer.start(config.minestom().host(), config.minestom().port());

        // Add shutdown hook for graceful shutdown
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            webServer.shutdown();
            ForkJoinPool.commonPool().awaitQuiescence(5, TimeUnit.SECONDS);
        });

        logger.info("Server started in {}ms", (System.nanoTime() - start) / 1_000_000);
    }

    private final CommandManager hubCommandManager = new CommandManager();
    private final CommandManager mapCommandManager = new CommandManager();

    private PlayerService playerService;
    private SessionService sessionService;
    private MapService mapService;
    private PermManager permManager;

    private boolean shuttingDown = false; // Used to run some things synchronously during shutdown.

    private DevHubServer hub;
    private DevMapServer maps;

    private MetricWriter metricWriter;

    public static Pattern onlinePlayersPattern = Pattern.compile("");
    public static final Sound TAG_DING = Sound.sound()
            .type(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP)
            .source(Sound.Source.PLAYER)
            .volume(5)
            .build();

    @Blocking
    public void start(@NotNull Config config, @NotNull NewConfigProvider configProvider) {
//        var velocityConfig = configProvider.get(VelocityConfig.class);
        var velocityConfig = new VelocityConfig(System.getenv("MAPMAKER_VELOCITY_SECRET"));
        if (velocityConfig.secret() != null && !velocityConfig.secret().isEmpty()) {
            logger.info("Enabling modern forwarding...");
            VelocityProxy.enable(velocityConfig.secret());
        } else {
            logger.info("Velocity not configured, using online mode...");
            MojangAuth.init();
        }

        // Start phase 1
        // Connect to low level services

        logger.info("Connecting to remote services...");

        var playerServiceUrl = System.getenv("MAPMAKER_PLAYER_SERVICE_URL");
        if (playerServiceUrl == null) playerServiceUrl = "http://localhost:9126";
        playerService = new PlayerServiceImpl(playerServiceUrl);

        var sessionServiceUrl = System.getenv("MAPMAKER_SESSION_SERVICE_URL");
        if (sessionServiceUrl == null) sessionServiceUrl = "http://localhost:9127";
        sessionService = new SessionServiceImpl(sessionServiceUrl);

        var mapServiceUrl = System.getenv("MAPMAKER_MAP_SERVICE_URL");
        if (mapServiceUrl == null) mapServiceUrl = "http://localhost:9125";
        mapService = new MapServiceImpl(mapServiceUrl);

        var spicedbUrl = System.getenv("MAPMAKER_SPICEDB_URL");
        if (spicedbUrl == null) spicedbUrl = "localhost:50051";
        var spicedbToken = System.getenv("MAPMAKER_SPICEDB_TOKEN");
        if (spicedbToken == null) spicedbToken = "supersecretkey";
        permManager = new PermManagerImpl(spicedbUrl, spicedbToken);

        var kafkaConfig = configProvider.get(KafkaConfig.class);
        new MapPlayerDataMgmtConsumer(kafkaConfig.bootstrapServersStr()); //todo close me
        metricWriter = new MetricWriter(kafkaConfig.bootstrapServersStr());

        var unleashAddress = System.getenv("MAPMAKER_UNLEASH_ADDRESS");
        var unleashToken = System.getenv("MAPMAKER_UNLEASH_TOKEN");
        if (unleashAddress != null && unleashToken != null) {
            var runtime = ServerRuntime.getRuntime();
            var unleashConfig = UnleashConfig.builder()
                    .appName("mapmaker")
                    .instanceId(runtime.hostname())
                    .unleashAPI(unleashAddress)
                    .apiKey(unleashToken)
                    .synchronousFetchOnInitialisation(true)
                    .build();

            var mapIds = new MapIdStrategy();
            UNLEASH_INSTANCE = new DefaultUnleash(unleashConfig, mapIds);
        }

        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            KindaBadThingToFix.badbadbad = player -> {
                var world = MapWorld.forPlayerOptional(player);
                return world == null ? null : world.map();
            };

            // Configure command rewriter
            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            var rewriter = new CommandRewriter(hubCommandManager, mapCommandManager);
            packetListenerManager.setListener(ClientCommandChatPacket.class, rewriter::execCommand);
            packetListenerManager.setListener(ClientTabCompletePacket.class, rewriter::tabCommand);
            MinecraftServer.getConnectionManager().setPlayerProvider(rewriter::createPlayer);

            packetListenerManager.setConfigurationListener(ClientResourcePackStatusPacket.class, this::handleResourcePackStatus);

            mapCommandManager.register(new HelpCommand(mapCommandManager));
//            hubCommandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage("no such command"));
//            mapCommandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage("no such command"));

            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, playerService, sessionService, mapService, permManager);
            this.maps = new DevMapServer(bridge, playerService, sessionService, mapService, permManager);
            bridge.setHubServer(hub);
            bridge.setMapServer(maps);
            this.hubToMapBridge = bridge;

            scope.fork(FutureUtil.call(() -> this.hub.init(hubCommandManager, maps.inviteService())));
            scope.fork(FutureUtil.call(() -> this.maps.init(configProvider, mapCommandManager)));

            scope.join();
        } catch (Exception e) {
            logger.error("failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var debugCommand = new DebugCommand(playerService, permManager, mapService);
            hubCommandManager.register(debugCommand);
            mapCommandManager.register(debugCommand);

            var emojisCommand = new EmojisCommand();
            mapCommandManager.register(emojisCommand);

            var hubMapCommand = hubCommandManager.getCommands().get("map");
            hubMapCommand.addSubcommand(new MapWorldCommand(maps.worldManager(), permManager));
            var mapMapCommand = mapCommandManager.getCommands().get("map");
            mapMapCommand.addSubcommand(new MapWorldCommand(maps.worldManager(), permManager));

            var eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
            eventHandler.addListener(AsyncPlayerConfigurationEvent.class, this::handleConfig);
            eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
            eventHandler.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
            eventHandler.addListener(PlayerSkinInitEvent.class, this::handleSkinInit);
            eventHandler.addListener(PlayerChatEvent.class, this::handleChatMessage);

            MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
            MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            var chatMessageListener = new ChatMessageListener(playerService, mapService, kafkaConfig.bootstrapServersStr());
            packetListenerManager.setListener(ClientChatMessagePacket.class, chatMessageListener);

            int i = 0;
            for (var facet : ServiceLoader.load(Facet.class)) {
                scope.fork(Executors.callable(() -> facet.hook(MinecraftServer.process(), configProvider)));
                i++;
            }
            logger.info("Loaded {} facets.", i);

            MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                logger.info("Graceful shutdown starting...");
                shuttingDown = true;
                chatMessageListener.close();
                hub.shutdown();
                maps.shutdown();
            });

            scope.join();
        } catch (Exception e) {
            logger.error("failed during startup", e);
            System.exit(1);
        }
    }

    public @NotNull List<HealthCheck> readinessChecks() {
        return List.of(() -> MinecraftServer.isStarted() ? HealthCheckResponse.up("minestom") : HealthCheckResponse.down("minestom"), () -> HealthCheckResponse.up("mapmaker"));
    }

    @Blocking
    private void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        // This event is executed off the main thread, so its ok to block here.
        var player = event.getPlayer();

        try {
            var playerData = sessionService.createSession(
                    event.getPlayerUuid().toString(),
                    event.getUsername(),
                    "todo"
            );
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

    private void handleResourcePackStatus(@NotNull ClientResourcePackStatusPacket packet, @NotNull Player player) {
        switch (packet.status()) {
            case INVALID_URL, FAILED_DOWNLOAD, FAILED_RELOAD -> player.kick("Resource pack failed to load.");
            case DECLINED -> player.kick("Resource pack declined.");
            case ACCEPTED, DISCARDED, DOWNLOADED -> {
                // Status messages
            }
            case SUCCESSFULLY_LOADED -> {
                var future = player.getTag(WAITING_FOR_RP_TAG);
                if (future != null) {
                    future.complete(null);
                    player.removeTag(WAITING_FOR_RP_TAG);
                }
            }
        }
    }

    private void handleConfig(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();
        logger.info("config - {}", player.getUsername());

        if (event.isFirstConfig()) {

            // Send resource pack if present
            var runtime = ServerRuntime.getRuntime();
            var resourcePackHash = ((DevRuntime) runtime).resourcePackSha1();
            if (!resourcePackHash.equals("dev")) {
                var url = String.format(RESOURCE_PACK_URL, runtime.commit());
                logger.info("Sending resource pack {} ({}) to {}", url, resourcePackHash, player.getUsername());
                player.setResourcePack(ResourcePack.forced(RESOURCE_PACK_ID, url, resourcePackHash)); //todo
                //todo in minestom it would be nice to have callback argument here to receive updates from resource pack status rather than having to do it on your own

                var future = new CompletableFuture<Void>();
                player.setTag(WAITING_FOR_RP_TAG, future);
                future.join();
            }
        }

        var targetWorld = player.getTag(MapHooks.TARGET_WORLD);
        if (targetWorld == null) {
            // Spawn into hub
            event.setSpawningInstance(hub.world().instance());
            player.setRespawnPoint(new Pos(0.5, 40, 0.5, 90, 0));
            return;
        }

        // Spawn the player into the map
        var imw = Objects.requireNonNull(FutureUtil.getUnchecked(targetWorld));

        // Resend registry containing the local biomes for this map
        var registry = new HashMap<String, NBT>();
        registry.put("minecraft:chat_type", Messenger.chatRegistry());
        registry.put("minecraft:dimension_type", MinecraftServer.getDimensionTypeManager().toNBT());
        registry.put("minecraft:worldgen/biome", imw.biomes().toNBT());
        registry.put("minecraft:damage_type", DamageType.getNBT());
        player.sendPacket(new RegistryDataPacket(NBT.Compound(registry)));

        player.sendPacket(new TagsPacket(MinecraftServer.getTagManager().getTagMap()));

        event.setSpawningInstance(imw.instance());
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        logger.info("disconnect - {}", event.getPlayer().getUsername());
        Runnable task = () -> {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);

            // There is just no need to do any of this if we arent shutting down.
            if (!shuttingDown) {
                Audiences.all().sendMessage(Component.translatable("chat.player.leave", playerData.displayName()));
                rebuildOnlinePlayersRegex();
                MiscFunctionality.broadcastTabList(Audiences.all());
            }

            try {
                sessionService.deleteSession(playerData.id());
            } catch (Exception e) {
                logger.error("Failed to close session for " + playerData.id(), e);
            }
        };

        if (shuttingDown) {
            task.run();
        } else {
            Thread.startVirtualThread(task);
        }
    }

    private void handleSkinInit(PlayerSkinInitEvent event) {
        event.setSkin(PlayerSkin.fromUuid(event.getPlayer().getUuid().toString()));
    }

    private static final Pattern EMOJI_REGEX = Pattern.compile(":([a-zA-Z\\-]+):");

    private void handleChatMessage(PlayerChatEvent event) {
        // Cancel the event, so we can send player specific versions ourselves
        event.setCancelled(true);

        var strippedMessage = FontUtil.stripInvalidChars(event.getMessage()).trim();
        if (strippedMessage.isBlank()) return;

        // Replace emojis with their sprites
        var baseMessage = Component.text(strippedMessage).replaceText(TextReplacementConfig.builder()
                .match(EMOJI_REGEX)
                .replacement((match, builder) -> {
                    var emoji = Emoji.findByName(match.group(1));
                    if (emoji == null) return builder;
                    return emoji.component();
                })
                .build());

        // If the message contains a map placeholder, and they are in a playing map, build the string to replace with
        var sender = event.getPlayer();
        if (event.getMessage().toLowerCase(Locale.ROOT).contains("[map]")) {
            var world = MapWorld.forPlayerOptional(sender);
            if (world == null || !world.map().isPublished()) {
                sender.sendMessage(Component.text("You are not in a published map.")); //todo message
                return;
            }

            var map = world.map();
            baseMessage = baseMessage.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("[map]")
                    //todo should also be a hypercube perk
                    .replacement((match, unused) -> MapData.createMapHoverText(map))
                    .build());

        }

        var lowerMessage = event.getMessage().toLowerCase(Locale.ROOT);
        for (var recipient : event.getRecipients()) {
            var message = baseMessage;

            // If they were tagged, send a ding effect and edit the message for them
            var namePattern = Pattern.compile(String.format("(?:^|\\s)(%s)", recipient.getUsername()), Pattern.CASE_INSENSITIVE);
            if (namePattern.matcher(lowerMessage).find()) {
                if (!recipient.equals(sender)) recipient.playSound(TAG_DING);
                message = message.replaceText(TextReplacementConfig.builder()
                        .match(namePattern)
                        .replacement((match, unused) -> Component.text(match.group(), TextColor.color(0xffe59e)))
                        .build());
            }

            recipient.sendMessage(Component.translatable(
                    "chat.channel.global.default",
                    PlayerDataV2.fromPlayer(event.getPlayer()).displayName(),
                    message
            ));
        }

    }

    private void rebuildOnlinePlayersRegex() {
        var builder = new StringBuilder();
        builder.append("(?:^|\\s)(");
        var first = true;
        for (var player : MinecraftServer.getConnectionManager().getPlayers(ConnectionState.PLAY)) {
            if (!first) builder.append("|");
            builder.append(player.getUsername());
            first = false;
        }
        builder.append(")");
        onlinePlayersPattern = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
    }

    Entity screenEntity = new Entity(EntityType.ITEM_DISPLAY) {{
        hasPhysics = false;
        setNoGravity(true);
    }};

    Entity lbTextEntity = new Entity(EntityType.TEXT_DISPLAY) {{
        hasPhysics = false;
        setNoGravity(true);
    }};

    Entity lbTextEntity2 = new Entity(EntityType.TEXT_DISPLAY) {{
        hasPhysics = false;
        setNoGravity(true);
    }};

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        // WARNING --------
        // IF YOU ADD SOMETHING HERE, IT PROBABLY ALSO NEEDS TO BE ADDED TO THE
        // SIMILAR FUNCTION IN HubServerImpl IN THE HUB BINARY MODULE.
        // WARNING --------
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        var playerData = PlayerDataV2.fromPlayer(player);

        player.setTeam(CoreTeams.DEFAULT); // todo do this based on rank
        MiscFunctionality.sendBetaHeader(player);

        // Resend the skin - TODO: this is a minestom bug, it should automatically resend metadata after reconfig but this is a temp fix.
        player.sendPacket(player.getMetadataPacket());


        var targetWorld = player.getTag(MapHooks.TARGET_WORLD);
        if (targetWorld != null) {
            player.setDisplayName(playerData.displayName()); //todo this is a Minestom bug, the display name needs to be re-sent automatically.
            MiscFunctionality.broadcastTabList(Audiences.all());

            var imw = FutureUtil.getUnchecked(targetWorld);
            if (false && imw instanceof PlayingMapWorld playingMapWorld) { // joinMapState == HubToMapBridge.JoinMapState.SPECTATING
                playingMapWorld.startSpectating(player, false);
            } else {
                imw.acceptPlayer(player, true);
            }
            return;
        }

        rebuildOnlinePlayersRegex();


        ActionBar.forPlayer(player).addProvider(MiscFunctionality::buildCurrencyDisplay);

        player.setDisplayName(playerData.displayName());
        Emoji.sendTabCompletions(player);
        MiscFunctionality.broadcastTabList(Audiences.all());

        Audiences.all().sendMessage(Component.translatable("chat.player.join", playerData.displayName()));

//        Thread.startVirtualThread(() -> {
//            metricWriter.writeMetric(new Metric(MetricType.PLAYER_JOIN_SERVER, List.of(System.currentTimeMillis(), player.getUuid())));
//        });


        // GARBAGE THAT NEEDS TO BE MOVED!!!

        var screenMeta = (ItemDisplayMeta) screenEntity.getEntityMeta();
        screenMeta.setItemStack(ItemStack.of(Material.STICK).withMeta(b -> b.customModelData(4)));
        screenEntity.setInstance(player.getInstance(), new Pos(-1, 46, -24)).join(); // should be -1
        screenMeta.setScale(new Vec(16, 16, 16));
        screenMeta.setLeftRotation(new Quaternion(new Vec(0, 0, 1).normalize(), Math.toRadians(10)).into());

        var lbTextMeta = (TextDisplayMeta) lbTextEntity.getEntityMeta();
        lbTextMeta.setText(Component.text()
                .append(Component.text("Leaderboard", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("#1 notmattw 100000")).appendNewline()
                .append(Component.text("#2 notmattw 100000")).appendNewline()
                .append(Component.text("#3 notmattw 100000")).appendNewline()
                .append(Component.text("#4 notmattw 100000")).appendNewline()
                .append(Component.text("#5 notmattw 100000")).appendNewline()
                .append(Component.text("#6 notmattw 100000")).appendNewline()
                .append(Component.text("#7 notmattw 100000")).appendNewline()
                .append(Component.text("#8 notmattw 100000")).appendNewline()
                .append(Component.text("#9 notmattw 100000")).appendNewline()
                .append(Component.text("#0 notmattw 100000"))
                .build());
        lbTextMeta.setBackgroundColor(0);
        lbTextEntity.setInstance(player.getInstance(), new Pos(5.97, 41, -25.4, 90, 0)).join();
        lbTextMeta.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(10)).into());
        lbTextMeta.setScale(new Vec(1.75));

        var lbTextMeta2 = (TextDisplayMeta) lbTextEntity2.getEntityMeta();
        lbTextMeta2.setText(Component.text()
                .append(Component.text("Leaderboard", NamedTextColor.GOLD)).appendNewline()
                .append(Component.text("#1 notmattw 100000")).appendNewline()
                .append(Component.text("#2 notmattw 100000")).appendNewline()
                .append(Component.text("#3 notmattw 100000")).appendNewline()
                .append(Component.text("#4 notmattw 100000")).appendNewline()
                .append(Component.text("#5 notmattw 100000")).appendNewline()
                .append(Component.text("#6 notmattw 100000")).appendNewline()
                .append(Component.text("#7 notmattw 100000")).appendNewline()
                .append(Component.text("#8 notmattw 100000")).appendNewline()
                .append(Component.text("#9 notmattw 100000")).appendNewline()
                .append(Component.text("#0 notmattw 100000"))
                .build());
        lbTextMeta2.setBackgroundColor(0);
        lbTextEntity2.setInstance(player.getInstance(), new Pos(5.97, 41, -19.8, 90, 0)).join();
        lbTextMeta2.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(10)).into());
        lbTextMeta2.setScale(new Vec(1.75));
    }

}

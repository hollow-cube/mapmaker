package net.hollowcube.mapmaker.dev;

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
import net.hollowcube.mapmaker.dev.chat.ChatMessageListener;
import net.hollowcube.mapmaker.dev.command.CommandRewriter;
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.dev.command.EmojisCommand;
import net.hollowcube.mapmaker.dev.command.map.MapWorldCommand;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.dev.config.NewConfigProvider;
import net.hollowcube.mapmaker.dev.http.HttpConfig;
import net.hollowcube.mapmaker.dev.runtime.DevRuntime;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricType;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PermManagerImpl;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.hollowcube.mapmaker.world.KindaBadThingToFix;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.PlayerSkin;
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
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.network.packet.server.common.TagsPacket;
import net.minestom.server.network.packet.server.configuration.RegistryDataPacket;
import net.minestom.server.resourcepack.ResourcePack;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public class DevServer {
    private static final Logger logger = LoggerFactory.getLogger(DevServer.class);

    private static final String RESOURCE_PACK_URL = "https://pub-620a83127bac451cbe2c402881b1b7d8.r2.dev/mapmaker-%s.zip";

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
    public static final Map<String, Component> EMOJIS;
    public static final Map<String, List<String>> EMOJIS_BY_CATEGORY;
    public static final Sound TAG_DING = Sound.sound()
            .type(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP)
            .source(Sound.Source.PLAYER)
            .volume(5)
            .build();

    static {
        var symbolEmojis = List.of(
                Map.entry("plus", "icon/plus"),
                Map.entry("minus", "icon/minus"),
                Map.entry("x", "icon/x_mark")
        );
        var faceEmojis = List.of(
                Map.entry("cool", "icon/emoji/cool"),
                Map.entry("grin", "icon/emoji/grin"),
                Map.entry("smile", "icon/emoji/smile"),
                Map.entry("smirk", "icon/emoji/smirk"),
                Map.entry("poop", "icon/emoji/poop")
        );
        var miscEmojis = List.of(
                Map.entry("crown", "icon/emoji/crown"),
                Map.entry("grass", "icon/emoji/grass"),
                Map.entry("sus", "icon/emoji/sus")
        );

        var allEmojis = new ArrayList<Map.Entry<String, String>>();
        allEmojis.addAll(symbolEmojis);
        allEmojis.addAll(faceEmojis);
        allEmojis.addAll(miscEmojis);
        Map<String, String> raw = Map.ofEntries(allEmojis.toArray(Map.Entry[]::new));

        var result = new HashMap<String, Component>();
        for (var entry : raw.entrySet()) {
            var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(entry.getValue()), entry.getValue());
            var hoverText = Component.text(":" + entry.getKey() + ":", NamedTextColor.WHITE);
            result.put(entry.getKey(), Component.text(sprite.fontChar(), FontUtil.NO_SHADOW).hoverEvent(HoverEvent.showText(hoverText)));
        }
        EMOJIS = Map.copyOf(result);

        var byCategory = new LinkedHashMap<String, List<String>>();
        byCategory.put("ѕʏᴍʙᴏʟѕ", List.copyOf(symbolEmojis.stream().map(Map.Entry::getKey).toList()));
        byCategory.put("ꜰᴀᴄᴇѕ", List.copyOf(faceEmojis.stream().map(Map.Entry::getKey).toList()));
        byCategory.put("ᴍɪѕᴄ", List.copyOf(miscEmojis.stream().map(Map.Entry::getKey).toList()));
        EMOJIS_BY_CATEGORY = byCategory;
    }

    @Blocking
    public void start(@NotNull Config config, @NotNull NewConfigProvider configProvider) {
        var velocitySecret = System.getenv("MAPMAKER_VELOCITY_SECRET");
        if (velocitySecret != null) {
            logger.info("Enabling velocity proxy...");
            VelocityProxy.enable(velocitySecret);
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

            hubCommandManager.register(new HelpCommand(hubCommandManager));
            mapCommandManager.register(new HelpCommand(mapCommandManager));
//            hubCommandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage("no such command"));
//            mapCommandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage("no such command"));
//
//            var arg = Argument.RelativeVec3("pos");
//            var command = new Command("fuck") {
//            };
//            command.addSyntax((sender, context) -> {
//                var newPos = context.get(arg);
//                lbTextEntity.teleport(Pos.fromPoint(newPos).withView(lbTextEntity.getPosition()));
//            }, arg);
//            hubCommandManager.register(command);
//
//            var arg2 = Argument.GreedyString("pos");
//            var command2 = new Command("fuck2") {
//            };
//            command2.addSyntax((sender, context) -> {
//                var newPos = context.get(arg2);
//                var m = (TextDisplayMeta) lbTextEntity.getEntityMeta();
//                m.setScale(new Vec(Double.parseDouble(newPos)));
////                lbTextEntity.teleport(Pos.fromPoint(newPos).withView(lbTextEntity.getPosition()));
//            }, arg2);
//            hubCommandManager.register(command2);

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
            hubCommandManager.register(emojisCommand);
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

    private void handleConfig(@NotNull AsyncPlayerConfigurationEvent event) {
        var player = event.getPlayer();
        logger.info("config - {}", player.getUsername());

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
                broadcastTabHeaderAndFooter();
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
                    var emoji = EMOJIS.get(match.group(1).toLowerCase(Locale.ROOT));
                    if (emoji == null) return builder;
                    return emoji;
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

    Entity smallHouseEntity = new Entity(EntityType.ITEM_DISPLAY) {{
        hasPhysics = false;
        setNoGravity(true);
    }};

    private boolean isStuffGoing = false;

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        var playerData = PlayerDataV2.fromPlayer(player);
        var runtime = ServerRuntime.getRuntime();

        // Add the player to the default team, todo do this based on rank
        player.setTeam(CoreTeams.DEFAULT);

        // Alpha watermark
        String watermarkString = String.format("play.hollowcube.net • Closed Beta (%s)", runtime.shortCommit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(FontUtil.NO_SHADOW), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));
        player.showBossBar(BossBar.bossBar(Component.text(FontUtil.rewrite("small_bossbar_line2", "not representative of final product"))
                .color(FontUtil.NO_SHADOW), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

        var targetWorld = player.getTag(MapHooks.TARGET_WORLD);
        if (targetWorld != null) {
            player.setDisplayName(playerData.displayName()); //todo this is a Minestom bug, the display name needs to be re-sent automatically.
            broadcastTabHeaderAndFooter();

            var imw = FutureUtil.getUnchecked(targetWorld);
            if (false && imw instanceof PlayingMapWorld playingMapWorld) { // joinMapState == HubToMapBridge.JoinMapState.SPECTATING
                playingMapWorld.startSpectating(player, false);
            } else {
                imw.acceptPlayer(player, true);
            }
            return;
        }

        rebuildOnlinePlayersRegex();

        //todo this gamemode/fly/permission level stuff should be handled by the hub server

        // Send resource pack if present
        var resourcePackHash = ((DevRuntime) runtime).resourcePackSha1();
        if (!resourcePackHash.equals("dev")) {
            var url = String.format(RESOURCE_PACK_URL, runtime.commit());
            logger.info("Sending resource pack {} ({}) to {}", url, resourcePackHash, player.getUsername());
            player.setResourcePack(ResourcePack.forced(url, resourcePackHash));
        }

        Audiences.all().sendMessage(Component.translatable("chat.player.join", playerData.displayName()));

        var currencyDisplay = BadSprite.SPRITE_MAP.get("hud/currency_display");
        var currencyDisplayCreative = BadSprite.SPRITE_MAP.get("hud/currency_display_creative");
        ActionBar.forPlayer(player).addProvider((p, builder) -> {
            var hasExperienceBar = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;

            builder.pushColor(FontUtil.NO_SHADOW);
            builder.pos(11).drawInPlace(hasExperienceBar ? currencyDisplay : currencyDisplayCreative);

            int MAX_TEXT_WIDTH = 22;
            var font = hasExperienceBar ? "currency" : "currency_creative";

            var coinText = NumberUtil.formatCurrency(playerData.coins());
            builder.pos(15 + (MAX_TEXT_WIDTH - FontUtil.measureText(font, coinText))).append(font, coinText);
            var cubitText = NumberUtil.formatCurrency(playerData.cubits());
            builder.pos(56 + (MAX_TEXT_WIDTH - FontUtil.measureText(font, cubitText))).append(font, cubitText);
//            builder.pos(56).append(font, "9.99b");
        });

        Thread.startVirtualThread(() -> {
            metricWriter.writeMetric(new Metric(MetricType.PLAYER_JOIN_SERVER, List.of(System.currentTimeMillis(), player.getUuid())));
        });

        player.setDisplayName(playerData.displayName());
        broadcastTabHeaderAndFooter();

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

        var smallHouseMeta = (ItemDisplayMeta) smallHouseEntity.getEntityMeta();
        smallHouseMeta.setItemStack(ItemStack.of(Material.STICK).withMeta(b -> b.customModelData(5)));
        smallHouseMeta.setScale(new Vec(4));
        smallHouseEntity.setInstance(player.getInstance(), new Pos(-38 + 0.5, 43, 54 + 0.5, 0, -90)).join();
//        smallHouseEntity.setInstance(player.getInstance(), player.getPosition().add(0, 5, 0)).join();
        smallHouseMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
//        smallHouseMeta.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(-90)).into());


        if (!isStuffGoing) {
            isStuffGoing = true;

            final var target = new AtomicInteger();
            final int duration = 5;
            MinecraftServer.getSchedulerManager().submitTask(() -> {
                smallHouseMeta.setNotifyAboutChanges(false);

                smallHouseMeta.setTransformationInterpolationStartDelta(0);
                smallHouseMeta.setTransformationInterpolationDuration(duration * 20);
//            smallHouseMeta.setTranslation(new Vec(0, ThreadLocalRandom.current().nextInt(5), 0));
                smallHouseMeta.setLeftRotation(new Quaternion(new Vec(0, 0, 1).normalize(), Math.toRadians(target.getAndAdd(90))).into());

                smallHouseMeta.setNotifyAboutChanges(true);
                return TaskSchedule.seconds(duration);
            });
        }

//        Scoreboards.showPlayerLobbyScoreboard(player);
//        Scoreboards.setScoreboardVisibility(player, Boolean.TRUE);
//        TabLists.showPlayerGlobalTabList(player);

//        var textEntity = new Entity(EntityType.TEXT_DISPLAY){{
//            hasPhysics = false;
//            setNoGravity(true);
//        }};
//        var textMeta = (TextDisplayMeta) textEntity.getEntityMeta();
//
//        var lbData = mapService.getPlaytimeLeaderboard("dec02f24-8f84-4e25-b0ea-2babcb62fc5c", "aceb326f-da15-45bc-bf2f-11940c21780c")
//                .toComponents(playerService, true);
//        if (lbData == null) {
//            textMeta.setText(Component.text("No leaderboard data"));
//        } else {
//            textMeta.setText(lbData.stream().reduce((a, b) -> a.append(Component.newline()).append(b)).orElse(Component.text("No leaderboard data")));
//        }
//        textMeta.setTextOpacity((byte) 0xFF);
//        textMeta.setScale(new Vec(1, 1, 1));
//
//        textEntity.setInstance(player.getInstance(), new Vec(0, 42, 0)).join();

//        var tube = new Entity(EntityType.ITEM_DISPLAY) {{
//            hasPhysics = false;
//            setNoGravity(true);
//        }};
//        var tubeMeta = (ItemDisplayMeta) tube.getEntityMeta();
//        tubeMeta.setItemStack(ItemStack.builder(Material.STICK).meta(b -> b.customModelData(1004)).build());
//        tubeMeta.setScale(new Vec(4, 4, 4));
//        tubeMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
//        //todo set width and height of model
//        tube.setInstance(hub.world().instance(), new Vec(0, 42, 0)).join();
//
//        var arm = new Entity(EntityType.ITEM_DISPLAY) {{
//            hasPhysics = false;
//        }};
//        arm.setNoGravity(true);
//        var armMeta = (ItemDisplayMeta) arm.getEntityMeta();
//        armMeta.setItemStack(ItemStack.builder(Material.STICK).meta(b -> b.customModelData(1005)).build());
//        armMeta.setScale(new Vec(4, 4, 4));
//        armMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.HEAD);
//        //todo set width and height of model
//        arm.setInstance(player.getInstance(), player.getPosition().sub(0, 31, 0)).join();
//        player.getInstance().setBlock(player.getPosition().sub(0, 31, 0), Block.TNT);
//
//        var pos = new AtomicDouble(0);
//        MinecraftServer.getSchedulerManager()
//                .buildTask(() -> {
//                    tubeMeta.setNotifyAboutChanges(false);
//                    armMeta.setNotifyAboutChanges(false);
//
//                    tubeMeta.setInterpolationDuration(80);
//                    armMeta.setInterpolationDuration(80);
//
//                    tubeMeta.setInterpolationStartDelta(1);
//                    armMeta.setInterpolationStartDelta(1);
//
//                    var newRot = pos.addAndGet(180) % 360;
//                    var rot = new Quaternion(new Vec(0, 1, 0), Math.toRadians(newRot));
//                    tubeMeta.setLeftRotation(rot.into());
//                    armMeta.setRightRotation(rot.into());
//
//                    if (newRot == 180) {
//                        armMeta.setTranslation(new Vec(0, 2, 0));
//                    } else {
//                        armMeta.setTranslation(new Vec(0, 0, 0));
//                    }
//
//                    tubeMeta.setNotifyAboutChanges(true);
//                    armMeta.setNotifyAboutChanges(true);
//                })
//                .delay(5, net.minestom.server.utils.time.TimeUnit.SECOND)
//                .repeat(5, net.minestom.server.utils.time.TimeUnit.SECOND)
//                .schedule();

    }

    private void broadcastTabHeaderAndFooter() {
        var onlinePlayers = MinecraftServer.getConnectionManager().getOnlinePlayerCount();
        var playersText = onlinePlayers == 1 ? "ᴘʟᴀʏᴇʀ" : "ᴘʟᴀʏᴇʀѕ";
        var playerCountText = FontUtil.rewrite("smallnums", "" + onlinePlayers);

        var blueColor = TextColor.color(56, 140, 249);
        var goldColor = TextColor.color(235, 188, 53);
        var darkGrayColor = TextColor.color(0x696969);
        var lightGrayColor = TextColor.color(0xB0B0B0); // or cccccc

        var tabLogoSprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/tab/logo_outline"));
        var cubeOffset = FontUtil.computeOffset(tabLogoSprite.width() + FontUtil.measureText(" Hollow Cube") - 50); //todo where is the missing 2 coming from
        var tabHeader = Component.text()
                .appendNewline()
                .append(Component.text(tabLogoSprite.fontChar(), FontUtil.NO_SHADOW).append(Component.text(" Hollow Cube", blueColor))).appendNewline()
                .append(Component.text(cubeOffset + "ᴄʟᴏѕᴇᴅ ʙᴇᴛᴀ", darkGrayColor))
                .appendNewline()
                .build();
        var tabFooter = Component.text()
                .appendNewline()
                .append(Component.text("ᴘʟᴀʏ.", lightGrayColor).append(Component.text("ʜᴏʟʟᴏᴡᴄᴜʙᴇ", goldColor)).append(Component.text(".ɴᴇᴛ", lightGrayColor))).appendNewline()
                .append(Component.text(playerCountText, blueColor).append(Component.text(" " + playersText + " ᴏɴʟɪɴᴇ", darkGrayColor))).appendNewline()
                .append(Component.text(FontUtil.computeOffset(125))) // Min width
                .build();

        Audiences.all().sendPlayerListHeaderAndFooter(tabHeader, tabFooter);
    }

}

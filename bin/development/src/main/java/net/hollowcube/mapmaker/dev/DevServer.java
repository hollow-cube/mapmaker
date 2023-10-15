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
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.command.*;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.dev.command.CommandRewriter;
import net.hollowcube.mapmaker.dev.command.DebugCommand;
import net.hollowcube.mapmaker.dev.config.Config;
import net.hollowcube.mapmaker.dev.config.NewConfigProvider;
import net.hollowcube.mapmaker.dev.http.HttpConfig;
import net.hollowcube.mapmaker.dev.runtime.DevRuntime;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapPlayerDataMgmtConsumer;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapServiceImpl;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricType;
import net.hollowcube.mapmaker.metrics.MetricWriter;
import net.hollowcube.mapmaker.player.*;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.NumberUtil;
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
import net.minestom.server.command.CommandManager;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket;
import net.minestom.server.resourcepack.ResourcePack;
import net.minestom.server.sound.SoundEvent;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/*

map_created (topic in kafka): { timestamp: time, player_id: uuid, size: int }


 */

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

    private DevHubServer hub;
    private DevMapServer maps;

    private MetricWriter metricWriter;

    private Pattern onlinePlayersPattern = Pattern.compile("");
    private static final Map<String, Component> EMOJIS;
    private static final Sound TAG_DING = Sound.sound()
            .type(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP)
            .source(Sound.Source.PLAYER)
            .volume(5)
            .build();

    static {
        var raw = Map.ofEntries(
                // Symbols
                Map.entry("plus", "icon/plus"),
                Map.entry("minus", "icon/minus"),
                Map.entry("x", "icon/x_mark"),

                // Faces
                Map.entry("cool", "icon/emoji/cool"),
                Map.entry("grin", "icon/emoji/grin"),
                Map.entry("smile", "icon/emoji/smile"),
                Map.entry("smirk", "icon/emoji/smirk"),

                // Misc
                Map.entry("crown", "icon/emoji/crown"),
                Map.entry("grass", "icon/emoji/grass")
        );

        var result = new HashMap<String, Component>();
        for (var entry : raw.entrySet()) {
            var sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(entry.getValue()), entry.getValue());
            var hoverText = Component.text(":" + entry.getKey() + ":", NamedTextColor.WHITE);
            result.put(entry.getKey(), Component.text(sprite.fontChar(), FontUtil.NO_SHADOW).hoverEvent(HoverEvent.showText(hoverText)));
        }
        EMOJIS = Map.copyOf(result);
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

        var kafkaConfig = configProvider.get(KafkaConfig.class);
        new MapPlayerDataMgmtConsumer(kafkaConfig.bootstrapServersStr()); //todo close me
        metricWriter = new MetricWriter(kafkaConfig.bootstrapServersStr());

        // Start phase 2
        // Start hub and map server and bridge them.

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Configure command rewriter
            var packetListenerManager = MinecraftServer.getPacketListenerManager();
            var rewriter = new CommandRewriter(hubCommandManager, mapCommandManager);
            packetListenerManager.setListener(ClientCommandChatPacket.class, rewriter::execCommand);
            packetListenerManager.setListener(ClientTabCompletePacket.class, rewriter::tabCommand);
            MinecraftServer.getConnectionManager().setPlayerProvider(rewriter::createPlayer);

            var bridge = new DevServerBridge();

            this.hub = new DevHubServer(bridge, playerService, sessionService, mapService);
            this.maps = new DevMapServer(bridge, playerService, sessionService, mapService);
            bridge.setHubServer(hub);
            bridge.setMapServer(maps);
            this.hubToMapBridge = bridge;

            scope.fork(FutureUtil.call(() -> this.hub.init(hubCommandManager)));
            scope.fork(FutureUtil.call(() -> this.maps.init(configProvider, mapCommandManager)));

            scope.join();
        } catch (Exception e) {
            logger.error("failed during startup", e);
            System.exit(1);
        }


        // Start phase 3
        // Load all facets & other misc startup tasks like setting up some events & minestom properties

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var debugCommand = new DebugCommand(playerService);
            hubCommandManager.register(debugCommand);
            mapCommandManager.register(debugCommand);

            var playCommand = new PlayCommand(mapService, hubToMapBridge);
            hubCommandManager.register(playCommand);
            mapCommandManager.register(playCommand);

            var whereCommand = new WhereCommand();
            hubCommandManager.register(whereCommand);
            mapCommandManager.register(whereCommand);

            var joinCommand = new JoinCommand();
            hubCommandManager.register(joinCommand);
            mapCommandManager.register(joinCommand);

            // Register Request/Accept/Reject to hub and map command managers
            var requestCommand = new RequestCommand();
            var acceptCommand = new AcceptCommand();
            var rejectCommand = new RejectCommand();
            hubCommandManager.register(requestCommand);
            hubCommandManager.register(acceptCommand);
            hubCommandManager.register(rejectCommand);
            mapCommandManager.register(requestCommand);
            mapCommandManager.register(acceptCommand);
            mapCommandManager.register(rejectCommand);

            var eventHandler = MinecraftServer.getGlobalEventHandler();
            eventHandler.addListener(AsyncPlayerPreLoginEvent.class, this::handlePreLogin);
            eventHandler.addListener(PlayerLoginEvent.class, this::handleLogin);
            eventHandler.addListener(PlayerSpawnEvent.class, this::handleFirstSpawn);
            eventHandler.addListener(PlayerDisconnectEvent.class, this::handleDisconnect);
            eventHandler.addListener(PlayerSkinInitEvent.class, this::handleSkinInit);
            eventHandler.addListener(PlayerChatEvent.class, this::handleChatMessage);

            MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true;
            MinestomAdventure.COMPONENT_TRANSLATOR = (component, locale) -> LanguageProviderV2.translate(component);

            int i = 0;
            for (var facet : ServiceLoader.load(Facet.class)) {
                scope.fork(Executors.callable(() -> facet.hook(MinecraftServer.process(), configProvider)));
                i++;
            }
            logger.info("Loaded {} facets.", i);

            MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
                logger.info("Graceful shutdown starting...");
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

    private void handleLogin(PlayerLoginEvent event) {
        event.setSpawningInstance(hub.world().instance());
        event.getPlayer().setRespawnPoint(new Pos(0.5, 40, 0.5, 90, 0));
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        Thread.startVirtualThread(() -> {
            var player = event.getPlayer();
            var playerData = PlayerDataV2.fromPlayer(player);

            Audiences.all().sendMessage(Component.translatable("chat.player.leave", playerData.displayName()));
            rebuildOnlinePlayersRegex();

            try {
                //todo we may want a dead letter or something, but im not sure where to put it. This requires a lot more thought
                sessionService.deleteSession(playerData.id());
            } catch (Exception e) {
                logger.error("Failed to close session for " + playerData.id(), e);
            }
        });
    }

    private void handleSkinInit(PlayerSkinInitEvent event) {
        event.setSkin(PlayerSkin.fromUuid(event.getPlayer().getUuid().toString()));
    }

    private void handleChatMessage(PlayerChatEvent event) {
        // Cancel the event, so we can send player specific versions ourselves
        event.setCancelled(true);

        // Replace emojis with their sprites
        var baseMessage = Component.text(event.getMessage()).replaceText(TextReplacementConfig.builder()
                .match(Pattern.compile(":([a-z\\-]+):"))
                .replacement((match, builder) -> {
                    var emoji = EMOJIS.get(match.group(1).toLowerCase(Locale.ROOT));
                    if (emoji == null) return builder;
                    return emoji;
                })
                .build());

        var lowerMessage = event.getMessage().toLowerCase(Locale.ROOT);
        for (var player : event.getRecipients()) {
            var message = baseMessage;

            // If they were tagged, send a ding effect and edit the message for them
            if (lowerMessage.contains(player.getUsername().toLowerCase(Locale.ROOT))) {
                if (!player.equals(event.getPlayer())) player.playSound(TAG_DING);
                message = message.replaceText(TextReplacementConfig.builder()
                        .match(Pattern.compile(String.format("(?:^|\\s)(%s)", player.getUsername()), Pattern.CASE_INSENSITIVE))
                        .replacement((match, unused) -> Component.text(match.group(), TextColor.color(0xffe59e)))
                        .build());
            }

            player.sendMessage(Component.translatable(
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
        for (var player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!first) builder.append("|");
            builder.append(player.getUsername());
            first = false;
        }
        builder.append(")");
        onlinePlayersPattern = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
    }

    private void handleFirstSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        rebuildOnlinePlayersRegex();

        //todo this gamemode/fly/permission level stuff should be handled by the hub server
        var player = event.getPlayer();

        // Send resource pack if present
        var runtime = ServerRuntime.getRuntime();
        var resourcePackHash = ((DevRuntime) runtime).resourcePackSha1();
        if (!resourcePackHash.equals("dev")) {
            var url = String.format(RESOURCE_PACK_URL, runtime.commit());
            logger.info("Sending resource pack {} ({}) to {}", url, resourcePackHash, player.getUsername());
            player.setResourcePack(ResourcePack.forced(url, resourcePackHash));
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        Audiences.all().sendMessage(Component.translatable("chat.player.join", playerData.displayName()));

        // Alpha watermark
        String watermarkString = String.format("MapMaker %s+%s, Not representative of final product", runtime.version(), runtime.shortCommit());
        player.showBossBar(BossBar.bossBar(Component.text(watermarkString).color(FontUtil.NO_SHADOW), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS));

        var currencyDisplay = BadSprite.SPRITE_MAP.get("hud/currency_display");
        var currencyDisplayCreative = BadSprite.SPRITE_MAP.get("hud/currency_display_creative");
        ActionBar.forPlayer(player).addProvider((p, builder) -> {
            var hasExperienceBar = p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE;

            builder.pushColor(FontUtil.NO_SHADOW);
            builder.pos(11).drawInPlace(hasExperienceBar ? currencyDisplay : currencyDisplayCreative);

            int MAX_TEXT_WIDTH = 22;
            var font = hasExperienceBar ? "currency" : "currency_creative";

            var coinText = NumberUtil.formatCurrency(999);
            builder.pos(15 + (MAX_TEXT_WIDTH - FontUtil.measureText(font, coinText))).append(font, coinText);
            builder.pos(56).append(font, "9.99b");
        });

        metricWriter.writeMetric(new Metric(MetricType.PLAYER_JOIN_SERVER, List.of(System.currentTimeMillis(), player.getUuid())));

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

}

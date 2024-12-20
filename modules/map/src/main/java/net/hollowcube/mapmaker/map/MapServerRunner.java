package net.hollowcube.mapmaker.map;

import net.hollowcube.command.CommandManager;
import net.hollowcube.command.util.HelpCommand;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.TopTimesCommand;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.event.FeatureFlagReloadEvent;
import net.hollowcube.mapmaker.kafka.BaseConsumer;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.block.InteractionRules;
import net.hollowcube.mapmaker.map.block.PlacementRules;
import net.hollowcube.mapmaker.map.block.handler.BlockHandlers;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.command.HubCommand;
import net.hollowcube.mapmaker.map.command.build.*;
import net.hollowcube.mapmaker.map.command.utility.*;
import net.hollowcube.mapmaker.map.command.utility.entity.EntitiesCommand;
import net.hollowcube.mapmaker.map.command.utility.navigation.*;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.hdb.command.HdbCommand;
import net.hollowcube.mapmaker.map.obungus.ObungusBoxReviewMap;
import net.hollowcube.mapmaker.map.runtime.*;
import net.hollowcube.mapmaker.map.terraform.MapServerModule;
import net.hollowcube.mapmaker.map.util.MapJoinInfo;
import net.hollowcube.mapmaker.map.util.MapPlayerImplImpl;
import net.hollowcube.mapmaker.map.world.AbstractMapMakerMapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.misc.ResourcePackManager;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.obungus.ObungusCore;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.timer.Scheduler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.hollowcube.mapmaker.map.util.MapCondition.eventFilter;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class MapServerRunner extends AbstractMapServer {
    private static final Logger logger = LoggerFactory.getLogger(MapServerRunner.class);

    private MapJoinConsumer mapJoinConsumer;
    private Terraform terraform;

    private FeatureList features;

    // A pending join is a map of player id to a future that will be completed when the server has the join info.
    // The future returned will never complete with an exception, but may complete with null indicating that we
    // timed out while waiting for the player info to be received.
    //
    // This will block the player from joining until the server has confirmed that it should have them and will
    // all happen during login (Minestom pre login event).
    // Note that we will separately hold them in the configuration phase until the map world is ready.
    private final Map<String, CompletableFuture<@Nullable MapJoinInfo>> pendingPlayerJoins = new ConcurrentHashMap<>();

    public MapServerRunner(@NotNull ConfigLoaderV3 config) {
        super(config);

        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("map-init")
                .addListener(AsyncPlayerConfigurationEvent.class, this::handleConfigPhase)
                .addListener(PlayerSpawnEvent.class, this::handleSpawn)
                .addListener(PlayerDisconnectEvent.class, this::handleDisconnect));
    }

    @Override
    protected @NotNull String name() {
        return "mapmaker-map";
    }

    public void addPendingJoin(@NotNull String playerId, @NotNull String mapId, @NotNull String state) {
        pendingPlayerJoins.put(playerId, CompletableFuture.completedFuture(new MapJoinInfo(playerId, mapId, state)));
    }

    @Override
    protected @NotNull MapAllocator createAllocator() {
        return new LocalMapAllocator(this);
    }

    @Override
    protected @NotNull ServerBridge createBridge() {
        return globalConfig.noop() ? new NoopServerBridge() : new MapServerBridge(this);
    }

    @Override
    protected void prepareStart() {
        super.prepareStart();

        MinecraftServer.getConnectionManager().setPlayerProvider((connection, gameProfile) -> new MapPlayerImplImpl(connection, gameProfile) {
            @Override
            public @NotNull CommandManager getCommandManager() {
                return commandManager();
            }
        });

        addBinding(Scheduler.class, MinecraftServer.getSchedulerManager());

        var kafkaConfig = config.get(KafkaConfig.class);
        if (!globalConfig.noop()) {
            mapJoinConsumer = new MapJoinConsumer(kafkaConfig.bootstrapServers());
            shutdowner().queue("map-join-listener", mapJoinConsumer::close);

            var mapMgmtConsumer = new MapMgmtConsumerImpl((LocalMapAllocator) allocator(), kafkaConfig.bootstrapServers());
            shutdowner().queue("map-mgmt-listener", mapMgmtConsumer::close);
        }

        this.terraform = initBuildLogic(mapService(), commandManager());
        addBinding(Terraform.class, terraform);

        var hdb = new HeadDatabase(otel);
        addBinding(HeadDatabase.class, hdb, "hdb");

        registerCommands(this, commandManager());

        initFeatureFlagMonitor(bridge(), allocator());

        this.features = FeatureList.load(config);
        addBinding(FeatureList.class, features);
        shutdowner().queue("features", features::close);
    }

    // Static so it can be referenced from dev server runner
    public static @NotNull Terraform initBuildLogic(@NotNull MapService mapService, @NotNull CommandManager commandManager) {
        var globalEventHandler = MinecraftServer.getGlobalEventHandler();

        // Create terraform instance
        var terraformEvents = EventNode.event("mapmaker:map/terraform", EventFilter.INSTANCE,
                eventFilter(false, true, false));
        globalEventHandler.addChild(terraformEvents);
        var terraform = Terraform.builder()
                .rootEventNode(terraformEvents)
                .rootCommandManager(commandManager)
                .globalCommandCondition(mapFilter(false, true, false))
                .module(Terraform.BASE_MODULE)
                .module(Terraform.AXIOM_MODULE)
                .module(Terraform.WORLDEDIT_MODULE)
                .module(MapServerModule::new)
                .storage(mapService instanceof NoopMapService ? "TerraformStorageMemory" : "TerraformStorageHttp")
                .build();
        //todo addListener for feature toggle refresh to disable terraform job execution
//        globalEventHandler.addListener(terraform

        // Block/item rules
        BlockHandlers.init();
        PlacementRules.init(terraform);
        var interactionEvents = EventNode.event("mapmaker:map/interaction", EventFilter.INSTANCE,
                eventFilter(false, true, false));
        interactionEvents.setPriority(10000000);
        globalEventHandler.addChild(interactionEvents);
        InteractionRules.register(interactionEvents);

        // Handle feature flag change to disable terraform if relevant
        if (!ServerRuntime.getRuntime().isDevelopment()) {
            terraform.setQueueLockState(MapFeatureFlags.TERRAFORM_DISABLE_TASKS.test());
            globalEventHandler.addListener(FeatureFlagReloadEvent.class, $ ->
                    terraform.setQueueLockState(MapFeatureFlags.TERRAFORM_DISABLE_TASKS.test()));
        }

        return terraform;
    }

    // Static so it can be referenced from dev server runner
    public static void registerCommands(@NotNull AbstractMapServer server, @NotNull CommandManager commandManager) {
        // Register two help commands. One for terraform commands, and one for regular.
        // We test terraform commands simply by checking if they start with / (eg // commands)
        commandManager.register(new HelpCommand(
                "help", new String[]{"h"},
                commandManager, CommandCategories.GLOBAL,
                entry -> !entry.getKey().startsWith("/")
        ));
        commandManager.register(new HelpCommand(
                "/help", new String[]{"/h"},
                commandManager, CommandCategories.GLOBAL,
                entry -> entry.getKey().startsWith("/")
        ));

        commandManager.register(server.createInstance(HubCommand.class));

        commandManager.register(server.createInstance(TopTimesCommand.class));

        commandManager.register(server.createInstance(TestCommand.class));
        commandManager.register(server.createInstance(BuilderMenuCommand.class));
        commandManager.register(server.createInstance(SetPreciseCoordsCommand.class));
        commandManager.register(server.createInstance(BuildCommand.class));
        commandManager.register(server.createInstance(SetSpawnCommand.class));

        commandManager.register(server.createInstance(FlyCommand.class));
        commandManager.register(server.createInstance(FlySpeedCommand.class));
        commandManager.register(server.createInstance(ClearInventoryCommand.class));
        commandManager.register(server.createInstance(SpawnCommand.class));
        commandManager.register(server.createInstance(GiveCommand.class));

        commandManager.register(server.createInstance(TeleportCommand.class));
        commandManager.register(server.createInstance(AscendCommand.class));
        commandManager.register(server.createInstance(DescendCommand.class));
        commandManager.register(server.createInstance(JumpToCommand.class));
        commandManager.register(server.createInstance(ThruCommand.class));
        commandManager.register(server.createInstance(UpCommand.class));

        commandManager.register(server.createInstance(PHeadCommand.class));
        commandManager.register(server.createInstance(HdbCommand.class));

        commandManager.register(server.createInstance(BiomesCommand.class));
//        commandManager.register(server.createInstance(SetBiomeCommand.class));

        commandManager.register(server.createInstance(AddMarkerCommand.class));
        commandManager.register(server.createInstance(EntitiesCommand.class));

        commandManager.register(server.createInstance(FixTheDripleafCommand.class));
    }

    public static void initFeatureFlagMonitor(@NotNull ServerBridge bridge, @NotNull MapAllocator allocator) {
        // TODO: This needs to be reimplemented using posthog flags. we can expose a callback after refresh.
        if (!(allocator instanceof LocalMapAllocator localAllocator)) return;
        MinecraftServer.getGlobalEventHandler().addListener(FeatureFlagReloadEvent.class, $ -> {
            if (!CoreFeatureFlags.MAP_DISABLE_ALL.test()) return;
            localAllocator.forEachWorld(world -> {
                if (!(world instanceof AbstractMapMakerMapWorld)) return;
                for (var player : world.players()) bridge.joinHub(player);
                for (var player : world.spectators()) bridge.joinHub(player);
            });
        });
    }

    protected void handleConfigPhase(@NotNull AsyncPlayerConfigurationEvent event) {
        try {
            var player = event.getPlayer();

            // Queue resource pack download/apply while we do other things
            var resourcePackFuture = ResourcePackManager.sendResourcePack(player);

            var playerId = player.getUuid().toString();
            var joinInfo = FutureUtil.getUnchecked(getPendingJoin(playerId, false));
            if (joinInfo == null) {
                logger.error("timed out waiting for join info for {}", playerId);
                player.kick(Component.text("Failed to join. Please try again later."));
                return;
            }

            var instanceId = ServerRuntime.getRuntime().hostname();
            var presence = new Presence(Presence.TYPE_MAPMAKER_MAP, joinInfo.state(), instanceId, joinInfo.mapId());
            transferPlayerSession(player, presence);

            // Create the world, holding the player here until it is ready for them to join.
            var map = mapService().getMap(joinInfo.playerId(), joinInfo.mapId());
            var mapWorld = Objects.requireNonNull(FutureUtil.getUnchecked(allocator().create(map, worldTypeFor(joinInfo))));

            // Ensure resource pack was applied before allowing the player in
            FutureUtil.getUnchecked(resourcePackFuture);
            if (!player.isOnline()) return;

            // Setup the player in the world
            mapWorld.configurePlayer(event);
        } catch (Exception e) {
            logger.error("Error during config phase", e);
            event.getPlayer().kick(Component.text("An unknown error has occurred. Please try again later."));
        }
    }

    protected @NotNull Class<? extends AbstractMapWorld> worldTypeFor(@NotNull MapJoinInfo joinInfo) {
        if (joinInfo.mapId().equals(ObungusCore.REVIEW_MAP_ID)) {
            return ObungusBoxReviewMap.class;
        }

        return Presence.MAP_BUILDING_STATES.contains(joinInfo.state())
                ? EditingMapWorld.class
                : PlayingMapWorld.class;
    }

    protected void handleSpawn(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;
        super.handleFirstSpawn(event.getPlayer());
    }

    protected void handleDisconnect(@NotNull PlayerDisconnectEvent event) {
        super.handlePlayerDisconnect(event.getPlayer());
    }

    @Override
    protected @NotNull DebugCommand createDebugCommand() {
        var cmd = super.createDebugCommand();

        cmd.createPermissionlessSubcommand("world", (player, context) -> {
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
            player.sendMessage("Type: " + world.getClass().getSimpleName());
        }, "Shows information about the world you are in");

        cmd.createPermissionedSubcommand("enableprogressaddition", (player, context) -> {
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            if (world instanceof EditingMapWorld && world.canEdit(player)) {
                world.map().setSetting(MapSettings.PROGRESS_INDEX_ADDITION, true);
                player.sendMessage("Enabled progress addition");
            } else {
                player.sendMessage("You are not in an editing world!");
            }
        }, "Enables progress index add mode for the current map");

        return cmd;
    }

    protected @NotNull CompletableFuture<@Nullable MapJoinInfo> getPendingJoin(@NotNull String playerId, boolean deleteCompleted) {
        if (mapService() instanceof NoopMapService) {
            //todo
            return CompletableFuture.completedFuture(new MapJoinInfo(
                    playerId,
                    "62da0aaf-8cad-4c13-869c-02b07688988d",
                    "editing"
            ));
        }

        var pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> {
            var future = new CompletableFuture<MapJoinInfo>();
            //todo the futures are never actually removed from the map.
            // invalid to do below because it will remove the future before we can handle it in login event.
//            future.whenComplete((v, e) -> pendingPlayerJoins.remove(id));
            return future;
        });
        if (deleteCompleted && pendingJoin.isDone()) {
            pendingPlayerJoins.remove(playerId);
            pendingJoin = pendingPlayerJoins.computeIfAbsent(playerId, id -> new CompletableFuture<>());
        }

        return pendingJoin;
    }

    private record MapJoinInfoMessage(
            @NotNull String serverId, @NotNull String playerId,
            @NotNull String mapId, @NotNull String state) {
    }

    private class MapJoinConsumer extends BaseConsumer<MapJoinInfoMessage> {

        protected MapJoinConsumer(@NotNull String bootstrapServers) {
            super("map-join", AbstractHttpService.hostname, s -> AbstractHttpService.GSON.fromJson(s, MapJoinInfoMessage.class), bootstrapServers);
        }

        @Override
        protected void onMessage(@NotNull ConsumerRecord<String, String> kafkaRecord, @NotNull MapJoinInfoMessage message) {
            if (!AbstractHttpService.hostname.equals(message.serverId())) return; // Not for this server, ignore.

            logger.info("received join info for {}: {}", message.playerId(), message);
            var pendingJoin = getPendingJoin(message.playerId(), true);
            pendingJoin.complete(new MapJoinInfo(message.playerId(), message.mapId(), message.state()));
        }
    }
}

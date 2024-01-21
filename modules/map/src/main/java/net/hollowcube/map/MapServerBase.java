package net.hollowcube.map;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandManager;
import net.hollowcube.map.biome.SetBiomeCommand;
import net.hollowcube.map.block.InteractionRules;
import net.hollowcube.map.block.PlacementRules;
import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.command.build.BiomesCommand;
import net.hollowcube.map.command.build.BuildCommand;
import net.hollowcube.map.command.build.SetSpawnCommand;
import net.hollowcube.map.command.build.TestCommand;
import net.hollowcube.map.command.invite.RemoveCommand;
import net.hollowcube.map.command.utility.*;
import net.hollowcube.map.entity.MapEntities;
import net.hollowcube.map.entity.potion.PotionHandler;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.invites.PlayerInviteServiceImpl;
import net.hollowcube.map.terraform.MapServerModule;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.command.EmojisCommand;
import net.hollowcube.mapmaker.command.MapCommand;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.command.TopTimesCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.store.StoreCommand;
import net.hollowcube.mapmaker.command.util.DebugCommand;
import net.hollowcube.mapmaker.command.util.MinestomCommand;
import net.hollowcube.mapmaker.command.util.PingCommand;
import net.hollowcube.mapmaker.command.util.WhereCommand;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import net.hollowcube.mapmaker.feature.unleash.UnleashConfig;
import net.hollowcube.mapmaker.feature.unleash.UnleashFeatureFlagProvider;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.misc.noop.NoopMapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.BlockManager;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static net.hollowcube.map.util.MapCondition.eventFilter;
import static net.hollowcube.map.util.MapCondition.mapFilter;

@SuppressWarnings("FieldCanBeLocal")
public abstract class MapServerBase implements MapServer {
    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();

    private static final Logger logger = LoggerFactory.getLogger(MapServerBase.class);

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map")
            .addListener(PlayerSpawnEvent.class, this::handleSpawn);

    private final MapWorldManager mwm = new MapWorldManager(this);
    private final PlayerInviteService inviteService = new PlayerInviteServiceImpl(mwm);

    private MapMgmtConsumerImpl mapMgmtConsumer;
    private List<FeatureProvider> features;

    private Controller guiController;

    // Terraform
    private Terraform terraform;

    private Injector injector;

    static {
        // Idk why the static initializer is not triggering from other usages
        //noinspection DataFlowIssue
        new PlayerSpawnInInstanceEvent(null);
    }

    public @Blocking void init(@NotNull ConfigLoaderV3 config, @NotNull CommandManager commandManager) {
        MapServer.StaticAbuse.instance = this;

        boolean noopServices = Boolean.getBoolean("mapmaker.noop");

        var unleashConfig = config.get(UnleashConfig.class);
        if (unleashConfig.enabled()) {
            logger.info("Unleash is enabled, loading feature flag provider");
            var provider = new UnleashFeatureFlagProvider(unleashConfig);
            FeatureFlagProvider.replaceGlobals(provider);
        }

        var globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addChild(eventNode);

        // Terraform initialization
        var terraformEvents = EventNode.event("mapmaker:map/terraform", EventFilter.INSTANCE,
                eventFilter(false, true, false));
        globalEventHandler.addChild(terraformEvents);
        terraform = Terraform.builder()
                .rootEventNode(terraformEvents)
                .rootCommandManager(commandManager)
                .globalCommandCondition(mapFilter(false, true, false))
                .module(Terraform.BASE_MODULE)
                .module(Terraform.AXIOM_MODULE)
                .module(MapServerModule::new)
                .storage(mapService() instanceof NoopMapService ? "memory" : "http")
                .build();

        this.guiController = Controller.make(Map.of(
                "mapServer", this,
                "mapService", mapService(),
                "playerService", playerService(),
                "bridge", bridge()
        ));

        this.injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MapServer.class).toInstance(MapServerBase.this);
                bind(MapServerBase.class).toInstance(MapServerBase.this);

                bind(MapWorldManager.class).toInstance(worldManager());
                bind(Controller.class).toInstance(guiController);
                bind(PlayerInviteService.class).toInstance(inviteService);
                bind(ConfigLoaderV3.class).toInstance(config);
                bind(PermManager.class).toInstance(permManager());
                bind(PlayerService.class).toInstance(playerService());
                bind(SessionManager.class).toInstance(sessionManager());
                bind(CommandManager.class).toInstance(commandManager);
                bind(MapService.class).toInstance(mapService());

                bind(MapToHubBridge.class).toInstance(bridge());
                bind(ServerBridge.class).toInstance(bridge());
            }
        });

        // Map management update listener
        var kafkaConfig = config.get(KafkaConfig.class);
        if (!noopServices) mapMgmtConsumer = new MapMgmtConsumerImpl(kafkaConfig.bootstrapServersStr(), this);

        // Block/item rules
        PlacementRules.init(terraform);
        var interactionEvents = EventNode.event("mapmaker:map/interaction", EventFilter.INSTANCE,
                eventFilter(false, true, false));
        globalEventHandler.addChild(interactionEvents);
        InteractionRules.register(interactionEvents);

        // Entities
        var entityEvents = EventNode.type("mapmaker:map/entity", EventFilter.INSTANCE);
        globalEventHandler.addChild(entityEvents);
        MapEntities.init(entityEvents);
        eventNode.addChild(PotionHandler.EVENT_NODE);

        // Common commands
//        commandManager.register(new HelpCommand(commandManager));
        commandManager.register(injector.getInstance(EmojisCommand.class));
        commandManager.register(injector.getInstance(MinestomCommand.class));
        commandManager.register(createDebugCommand());
        commandManager.register(injector.getInstance(PingCommand.class));

        commandManager.register(injector.getInstance(PlayCommand.class));
        commandManager.register(injector.getInstance(WhereCommand.class));
        commandManager.register(injector.getInstance(TopTimesCommand.class));

        commandManager.register(injector.getInstance(RequestCommand.class));
        commandManager.register(injector.getInstance(RejectCommand.class));
        commandManager.register(injector.getInstance(InviteCommand.class));
        commandManager.register(injector.getInstance(AcceptCommand.class));
        commandManager.register(injector.getInstance(JoinCommand.class));
        commandManager.register(injector.getInstance(RemoveCommand.class));

        var mapCommand = injector.getInstance(MapCommand.class);
//        mapCommand.info.addSyntax(CommandDsl.playerOnly(MapListCommandMixin::showMapInfoAboutCurrent));
        commandManager.register(mapCommand);

        commandManager.register(injector.getInstance(StoreCommand.class));

        // Map specific commands
        commandManager.register(injector.getInstance(HubCommand.class));

        commandManager.register(injector.getInstance(TestCommand.class));
        commandManager.register(injector.getInstance(BuildCommand.class));
        commandManager.register(injector.getInstance(SetSpawnCommand.class));

        commandManager.register(injector.getInstance(FlyCommand.class));
        commandManager.register(injector.getInstance(FlySpeedCommand.class));
        commandManager.register(injector.getInstance(ClearInventoryCommand.class));
        commandManager.register(injector.getInstance(SpawnCommand.class));
        commandManager.register(injector.getInstance(TeleportCommand.class));
        commandManager.register(injector.getInstance(GiveCommand.class));

        commandManager.register(injector.getInstance(PHeadCommand.class));

        commandManager.register(injector.getInstance(BiomesCommand.class));
        commandManager.register(injector.getInstance(SetBiomeCommand.class));

        // Register features
        var features = new ArrayList<FeatureProvider>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (var feature : ServiceLoader.load(FeatureProvider.class)) {
                features.add(feature);
                for (var blockHandler : feature.blockHandlers()) {
                    BLOCK_MANAGER.registerHandler(blockHandler.getNamespaceId(), () -> blockHandler);
                }
                scope.fork(Executors.callable(() -> feature.init(config)));
            }

            scope.join();
            this.features = List.copyOf(features);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Failed to initialize features", e);
            throw new RuntimeException(e);
        }

        // Sync sessions with remote
        sessionManager().sync();
    }

    @Override
    public @NotNull List<FeatureProvider> features() {
        if (features == null) {
            return List.of();
        }
        return features;
    }

    @Override
    public @NotNull Terraform terraform() {
        return this.terraform;
    }

    public @NotNull MapWorldManager worldManager() {
        return mwm;
    }

    public @NotNull PlayerInviteService inviteService() {
        return inviteService;
    }

    public @Blocking void joinMap(@NotNull Player player, @NotNull MapData map, HubToMapBridge.JoinMapState joinMapState) {
        mwm.joinMap(player, map, joinMapState);
    }

    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
        // Spawn event is not an InstanceEvent, so we need to filter it.
        if (MapWorld.unsafeFromInstance(event.getSpawnInstance()) == null)
            return;

        var player = event.getPlayer();
        player.refreshCommands();

        // This is invalid because the player has not actually entered the map, so forPlayer fails.
//        var map = MapWorld.forPlayer(event.getPlayer()).map();
//        if (map.isPublished()) {
//            Scoreboards.showPlayerPlayingScoreboard(player, map);
//        } else {
//            Scoreboards.showPlayerEditingScoreboard(player, map);
//        }
    }

    @Override
    public void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController.show(player, viewProvider);
    }

    public void shutdown() {
        mapMgmtConsumer.close();
        mwm.shutdown();
    }

    private @NotNull DebugCommand createDebugCommand() {
        var cmd = new DebugCommand(playerService(), permManager(), mapService());

        cmd.createPermissionlessSubcommand("world", (player, context) -> {
            var world = MapWorld.forPlayerOptional(player);
            if (world == null) {
                player.sendMessage("You are not in a map world!");
                return;
            }

            player.sendMessage(Component.text("Map: ").append(Component.text(world.map().id())));
            player.sendMessage("Type: " + world.getClass().getSimpleName());
        });

        return cmd;
    }

}

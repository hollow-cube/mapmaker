package net.hollowcube.map;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.Command;
import net.hollowcube.command.CommandManager;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.map.block.handler.*;
import net.hollowcube.map.block.placement.PlacementRules;
import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.command.MapListCommandMixin;
import net.hollowcube.map.command.build.BuildCommand;
import net.hollowcube.map.command.build.SetSpawnCommand;
import net.hollowcube.map.command.build.TestCommand;
import net.hollowcube.map.command.invite.RemoveCommand;
import net.hollowcube.map.command.utility.*;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.invites.PlayerInviteServiceImpl;
import net.hollowcube.map.terraform.MapServerModule;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.command.MapCommand;
import net.hollowcube.mapmaker.command.PlayCommand;
import net.hollowcube.mapmaker.command.TopTimesCommand;
import net.hollowcube.mapmaker.command.invite.*;
import net.hollowcube.mapmaker.command.util.WhereCommand;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.kafka.KafkaConfig;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.TerraformOldInit;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.listener.manager.PacketListenerManager;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.function.Function;

public abstract class MapServerBase implements MapServer {
    private static final PacketListenerManager PACKET_LISTENER_MANAGER = MinecraftServer.getPacketListenerManager();
    private static final BlockManager BLOCK_MANAGER = MinecraftServer.getBlockManager();

    private static final System.Logger logger = System.getLogger(MapServerBase.class.getName());

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map");

    private final MapWorldManager mwm = new MapWorldManager(this);
    private final MapToHubBridge bridge;
    private PlayerInviteService inviteService = new PlayerInviteServiceImpl(mwm);

    private MapMgmtConsumerImpl mapMgmtConsumer;
    private List<FeatureProvider> features;

    private Controller guiController;

    // Terraform
    private final Terraform terraform = Terraform.builder()
            .module(Terraform.BASE_MODULE)
            .module(new MapServerModule())
            .storage("http")
            .build();

    static {
        // Idk why the static initializer is not triggering from other usages
        new PlayerSpawnInInstanceEvent(null);
    }

    public MapServerBase(@NotNull MapToHubBridge bridge) {
        this.bridge = bridge;
    }

    public @Blocking void init(@NotNull ConfigProvider config, @NotNull CommandManager commandManager) {
        MapServer.StaticAbuse.instance = this;

        var kafkaConfig = config.get(KafkaConfig.class);
        mapMgmtConsumer = new MapMgmtConsumerImpl(kafkaConfig.bootstrapServersStr(), this);

        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(MapWorldUnregisterEvent.class, this::handleMapUnregister);
//        eventNode.addListener(PlayerBlockPlaceEvent.class, EditWorldPlaceBlockEvent::handleBlockPlacement);

        // Placement rules
        PlacementRules.init();
        BLOCK_MANAGER.registerHandler(SignBlockHandler.ID, () -> SignBlockHandler.INSTANCE);
        BLOCK_MANAGER.registerHandler(PlayerHeadBlockHandler.ID, () -> PlayerHeadBlockHandler.INSTANCE);
        BLOCK_MANAGER.registerHandler(ChestBlockHandler.CHEST.getNamespaceId(), () -> ChestBlockHandler.CHEST);
        BLOCK_MANAGER.registerHandler(ChestBlockHandler.TRAPPED_CHEST.getNamespaceId(), () -> ChestBlockHandler.TRAPPED_CHEST);
        BLOCK_MANAGER.registerHandler(ShulkerBoxBlockHandler.ID, () -> ShulkerBoxBlockHandler.INSTANCE);
        BLOCK_MANAGER.registerHandler(BannerBlockHandler.INSTANCE.getNamespaceId(), () -> BannerBlockHandler.INSTANCE);
        PACKET_LISTENER_MANAGER.setListener(ClientUpdateSignPacket.class, SignBlockHandler::handleUpdateSignPacket);

        // Terraform initialization
        var terraformEvents = EventNode.value("mapmaker:map/terraform", EventFilter.INSTANCE,
                instance -> MapWorld.unsafeFromInstance(instance) != null);
        MinecraftServer.getGlobalEventHandler().addChild(terraformEvents);
        TerraformOldInit.init(commandManager, terraformEvents, null, terraform);
//        TerraformCompat.init(terraformEvents, condition);
//        TerraformAxiom.init(terraformEvents, null);

        // Common commands
        commandManager.register(new PlayCommand(mapService(), bridge()));
        commandManager.register(new WhereCommand());
        commandManager.register(new TopTimesCommand(mapService(), playerService()));

        commandManager.register(new RequestCommand(inviteService));
        commandManager.register(new RejectCommand(inviteService));
        commandManager.register(new InviteCommand(inviteService));
        commandManager.register(new AcceptCommand(inviteService));
        commandManager.register(new JoinCommand(inviteService));
        commandManager.register(new RemoveCommand(bridge));

        var mapCommand = new MapCommand(guiController, playerService(), mapService(), permManager());
        mapCommand.info.setDefaultExecutor(Command.playerOnly(MapListCommandMixin::showMapInfoAboutCurrent));
        commandManager.register(mapCommand);

        // Map specific commands
        commandManager.register(new HubCommand(bridge, inviteService));

        commandManager.register(new TestCommand());
        commandManager.register(new BuildCommand());
        commandManager.register(new SetSpawnCommand());

        commandManager.register(new FlyCommand());
        commandManager.register(new FlySpeedCommand());
        commandManager.register(new ClearInventoryCommand());
        commandManager.register(new SpawnCommand());
        commandManager.register(new TeleportCommand());
        commandManager.register(new GiveCommand());

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
            logger.log(System.Logger.Level.ERROR, "Failed to initialize features", e);
            throw new RuntimeException(e);
        }

        this.guiController = Controller.make(Map.of(
                "mapServer", this,
                "mapService", mapService(),
                "bridge", bridge()
        ));
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

    @Override
    public @NotNull MapToHubBridge bridge() {
        return bridge;
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

    private void handleMapUnregister(@NotNull MapWorldUnregisterEvent event) {
//        var activeMaps = maps.get(event.getMap().getId());
//        if (activeMaps == null) {
//            // Something went wrong and the instance is not registered
//            logger.log(System.Logger.Level.ERROR, "Attempted to unregister {}, but it was not registered.", event.getMap().getId());
//            return;
//        }
//
//        var removed = activeMaps.remove(event.mapWorld().getClass());
//        if (removed == null) {
//            // Something went wrong and the instance is not registered
//            logger.log(System.Logger.Level.ERROR, "Attempted to unregister {}, but it was not registered.", event.getMap().getId());
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


}

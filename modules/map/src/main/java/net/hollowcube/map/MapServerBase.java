package net.hollowcube.map;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.map.block.handler.SignBlockHandler;
import net.hollowcube.map.block.rule.PlacementRules;
import net.hollowcube.map.command.*;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
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

    private final MapToHubBridge bridge;

    private List<FeatureProvider> features;

    private Controller guiController;

    static {
        // Idk why the static initializer is not triggering from other usages
        new PlayerSpawnInInstanceEvent(null);
    }

    public MapServerBase(@NotNull MapToHubBridge bridge) {
        this.bridge = bridge;
    }

    public @Blocking void init(@NotNull ConfigProvider config) {
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(MapWorldUnregisterEvent.class, this::handleMapUnregister);
//        eventNode.addListener(PlayerBlockPlaceEvent.class, EditWorldPlaceBlockEvent::handleBlockPlacement);

        // Placement rules
        PlacementRules.init();
        BLOCK_MANAGER.registerHandler(SignBlockHandler.ID, () -> SignBlockHandler.INSTANCE);
        PACKET_LISTENER_MANAGER.setListener(ClientUpdateSignPacket.class, SignBlockHandler::handleUpdateSignPacket);

        // Terraform initialization
        var terraformEvents = EventNode.value("mapmaker:map/terraform", EventFilter.INSTANCE,
                instance -> MapWorld.unsafeFromInstance(instance) != null);
        MinecraftServer.getGlobalEventHandler().addChild(terraformEvents);
//        Terraform.init(terraformEvents, BaseMapCommand.createMapCondition(true));
//        TerraformCompat.init(terraformEvents, BaseMapCommand.createMapCondition(true));
        Terraform.init(terraformEvents, null);
//        TerraformCompat.init(terraformEvents, null);

        // Register commands
        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new HubCommand(bridge));
        commandManager.register(new GiveCommand());
        commandManager.register(new SetSpawnCommand());
        commandManager.register(new ClearInventoryCommand());
        commandManager.register(new FlyCommand());
        commandManager.register(new FlySpeedCommand());
        commandManager.register(new TeleportCommand());
        commandManager.register(new TestModeCommand(this));
        commandManager.register(new BuildModeCommand(this));

        // Register features
        var features = new ArrayList<FeatureProvider>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (var feature : ServiceLoader.load(FeatureProvider.class)) {
                features.add(feature);
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
                "mapService", mapService()
        ));
    }

    @Override
    public @NotNull List<FeatureProvider> features() {
        if (features == null) {
            return List.of();
        }
        return features;
    }

    private final MapWorldManager mwm = new MapWorldManager(this);

    public @Blocking void joinMap(@NotNull Player player, @NotNull MapData map, boolean isEditing) {
        mwm.joinMap(player, map, isEditing);

//        var activeMaps = maps.computeIfAbsent(map.getId(), id -> new ConcurrentHashMap<>());
//
//        // Search for a world with the same flags
//        var activeWorld = activeMaps.get(isEditing ? EditingMapWorld.class : PlayingMapWorld.class);
//        if (activeWorld != null) {
//            return FutureResult.wrap(player.setInstance(activeWorld.instance(), new Pos(0.5, 60, 0.5)));
//        }
//
//        // No such map, create a new one
//        MapWorld world = isEditing ? new EditingMapWorld(this, map) : new PlayingMapWorld(this, map);
//        activeMaps.put(world.getClass(), world);
//
//        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
//        if (map.getMapFileId() != null)
//            future = world.loadWorld();
//        return FutureResult.wrap(future.thenCompose(unused ->
//                player.setInstance(world.instance(), new Pos(0.5, 60, 0.5))));
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

}

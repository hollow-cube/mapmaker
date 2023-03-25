package net.hollowcube.map;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.block.placement.HCPlacementRules;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.Section;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.map.command.*;
import net.hollowcube.map.event.EditWorldPlaceBlockEvent;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.map.feature2.FeatureProvider;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.config.ConfigProvider;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.compat.TerraformCompat;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MapServerBase implements MapServer {
    public static final Logger logger = LoggerFactory.getLogger(MapServer.class);

    private final EventNode<Event> eventNode = EventNode.all("mapmaker:map");

    private final MapToHubBridge bridge;

    private List<FeatureProvider> features;


    // map id -> play|edit -> world
    // Used to send players to the same world if there is already one instance of it.
    private final Map<String, Map<Class<?>, MapWorld>> maps = new ConcurrentHashMap<>();

    static {
        // Idk why the static initializer is not triggering from other usages
        new PlayerSpawnInInstanceEvent(null);
    }

    public MapServerBase(@NotNull MapToHubBridge bridge) {
        this.bridge = bridge;
    }

    public @NotNull ListenableFuture<Void> init(@NotNull ConfigProvider config) {
        MinecraftServer.getGlobalEventHandler().addChild(eventNode);
        eventNode.addListener(PlayerSpawnEvent.class, this::handleSpawn);
        eventNode.addListener(MapWorldUnregisterEvent.class, this::handleMapUnregister);
        eventNode.addListener(PlayerBlockPlaceEvent.class, EditWorldPlaceBlockEvent::handleBlockPlacement);

        // Placement rules
        var blockEvents = EventNode.type("placement_rules_map", EventFilter.BLOCK, (event, unused) -> {
            if (event instanceof InstanceEvent instanceEvent)
                return instanceEvent.getInstance().hasTag(MapWorld.MAP_ID);
            return false;
        });
        MinecraftServer.getGlobalEventHandler().addChild(blockEvents);
        HCPlacementRules.init(blockEvents);

        // Terraform initialization
        var terraformEvents = EventNode.value("mapmaker:map/terraform", EventFilter.INSTANCE,
                instance -> instance.hasTag(MapWorld.MAP_ID));
        MinecraftServer.getGlobalEventHandler().addChild(terraformEvents);
//        Terraform.init(terraformEvents, BaseMapCommand.createMapCondition(true));
//        TerraformCompat.init(terraformEvents, BaseMapCommand.createMapCondition(true));
        Terraform.init(terraformEvents, null);
        TerraformCompat.init(terraformEvents, null);

        // Register commands
        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new HubCommand(bridge));
        commandManager.register(new GiveCommand());
        commandManager.register(new SetSpawnCommand());
        commandManager.register(new ClearInventoryCommand());
        commandManager.register(new FlyCommand());
        commandManager.register(new FlySpeedCommand());
        commandManager.register(new TeleportCommand());
        commandManager.register(new MultiBuildCommand());

        // Register features
        var features = new ArrayList<FeatureProvider>();
        var featureInitFutures = new ArrayList<ListenableFuture<Void>>();
        for (var feature : ServiceLoader.load(FeatureProvider.class)) {
            features.add(feature);
            featureInitFutures.add(feature.init(config));
        }
        this.features = List.copyOf(features);

        return Futures.transform(
                Futures.allAsList(featureInitFutures),
                unused -> null, Runnable::run);
    }

    @Override
    public @NotNull List<FeatureProvider> features() {
        return features;
    }

    public @NotNull FutureResult<Void> joinMap(@NotNull Player player, @NotNull MapData map, boolean isEditing) {
        var activeMaps = maps.computeIfAbsent(map.getId(), id -> new ConcurrentHashMap<>());

        // Search for a world with the same flags
        var activeWorld = activeMaps.get(isEditing ? EditingMapWorld.class : PlayingMapWorld.class);
        if (activeWorld != null) {
            return FutureResult.wrap(player.setInstance(activeWorld.instance(), new Pos(0.5, 60, 0.5)));
        }

        // No such map, create a new one
        MapWorld world = isEditing ? new EditingMapWorld(this, map) : new PlayingMapWorld(this, map);
        activeMaps.put(world.getClass(), world);

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        if (map.getMapFileId() != null)
            future = world.loadWorld();
        return FutureResult.wrap(future.thenCompose(unused ->
                player.setInstance(world.instance(), new Pos(0.5, 60, 0.5))));
    }

    private void handleSpawn(@NotNull PlayerSpawnEvent event) {
        // Spawn event is not an InstanceEvent, so we need to filter it.
        if (!event.getSpawnInstance().hasTag(MapWorld.MAP_ID))
            return;

        var player = event.getPlayer();
        player.refreshCommands();
    }

    private void handleMapUnregister(@NotNull MapWorldUnregisterEvent event) {
        var activeMaps = maps.get(event.getMap().getId());
        if (activeMaps == null) {
            // Something went wrong and the instance is not registered
            logger.error("Attempted to unregister {}, but it was not registered.", event.getMap().getId());
            return;
        }

        var removed = activeMaps.remove(event.mapWorld().getClass());
        if (removed == null) {
            // Something went wrong and the instance is not registered
            logger.error("Attempted to unregister {}, but it was not registered.", event.getMap().getId());
        }
    }

    @Override
    public void openGUIForPlayer(@NotNull Player player, @NotNull Section gui) {
        //todo context
        new RouterSection(gui).showToPlayer(player);
    }
}

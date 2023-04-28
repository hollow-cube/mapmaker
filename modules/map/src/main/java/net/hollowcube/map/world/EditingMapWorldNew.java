package net.hollowcube.map.world;

import jdk.incubator.concurrent.StructuredTaskScope;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldPlayerStartPlayingEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.util.StringUtil;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.generation.MapGenerators;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

public class EditingMapWorldNew implements InternalMapWorldNew {
    private static final System.Logger logger = System.getLogger(EditingMapWorldNew.class.getName());

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_EDITING = Tag.Boolean("editing").defaultValue(false);

    private final MapServer server;
    private final MapData map;
    private int flags = 0;

    private final BaseWorld baseWorld;
    private TestingMapWorldNew testWorld = null;

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> eventNode = EventNode.event("world-local", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_EDITING);
        }
        return true;
    });

    public EditingMapWorldNew(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.map = map;
        this.flags |= FLAG_EDITING;

        var instance = new InstanceContainer(StringUtil.seededUUID(map.getId()), DimensionTypes.FULL_BRIGHT);
        this.baseWorld = new BaseWorld(server.worldManager(), map.getId(), instance);
        instance.setGenerator(MapGenerators.voidWorld());
        instance.setTag(SELF_TAG, this);

        this.itemRegistry = new ItemRegistry();
    }

    @Override
    public @NotNull MapServer server() {
        return server;
    }

    @Override
    public @NotNull MapData map() {
        return map;
    }

    @Override
    public int flags() {
        return flags;
    }

    @Override
    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {
        eventNode.addChild(eventNode);
    }

    @Override
    public @NotNull Point spawnPoint() {
        return new Vec(0.5, 40, 0.5);
    }

    @Override
    public @NotNull Instance instance() {
        return baseWorld.instance();
    }

    @Override
    public @Blocking void load() {
        // Load the map itself (eg blocks, if present)
        if (map.getMapFileId() != null) {
            baseWorld.loadWorld();
        }

        // Load each of the features
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Load each feature in parallel
            var features = server.features();
            var enabledFutures = new Future[features.size()];
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                enabledFutures[i] = scope.fork(() -> feature.initMap(this));
            }

            scope.join();

            // Add each feature to the enabled list if it is enabled.
            for (int i = 0; i < features.size(); i++) {
                var feature = features.get(i);
                if ((boolean) enabledFutures[i].resultNow()) {
                    enabledFeatures.add(feature);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public @Blocking void close() {
        if (testWorld != null) {
            testWorld.close();
        }

        // Save the backing world (blocks, etc)
        var fileId = baseWorld.saveWorld();

        // Update the map with the new file id & save it
        map.setMapFileId(fileId);
        server.mapStorage().updateMap(map);

        // Unload the backing world
        baseWorld.unloadWorld();
    }

    @Override
    public @Blocking void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);

        player.setTag(TAG_EDITING, true);
        player.setGameMode(GameMode.CREATIVE); //todo based on setting
        player.refreshCommands();

        var saveStateStorage = server.saveStateStorage();
        SaveState saveState;
        try {
            saveState = saveStateStorage.getLatestSaveState(playerData.getId(), map().getId(), SaveState.Type.EDITING);
        } catch (SaveStateStorage.NotFoundError e) {
            // common savestate creation logic todo move elsewhere
            saveState = new SaveState();
            saveState.setId(UUID.randomUUID().toString());
            saveState.setPlayerId(playerData.getId());
            saveState.setMapId(map().getId());
            saveState.setStartTime(Instant.now());
            saveState.setPos(map.getSpawnPoint());

            saveState.setType(SaveState.Type.EDITING);

            saveStateStorage.createSaveState(saveState);

            player.setTag(MapHooks.PLAYING, true);
            player.setTag(SaveState.TAG, saveState);
        }

        // formerly initPlayerFromSaveState
        var inventory = saveState.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            player.getInventory().setItemStack(i, inventory.get(i));
        }
        player.setGameMode(GameMode.CREATIVE);
        player.teleport(saveState.getPos()).join();
        player.sendMessage("Now editing " + map.getName());

        EventDispatcher.call(new MapWorldPlayerStartPlayingEvent(this, player));
    }

    @Override
    public @Blocking void removePlayer(@NotNull Player player) {
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {

            // Formerly updateSaveStateForPlayer
            saveState.setPos(player.getPosition());
            saveState.setInventory(List.of(player.getInventory().getItemStacks()));

            try {
                var saveStateStorage = server.saveStateStorage();
                saveStateStorage.updateSaveState(saveState);
                logger.log(System.Logger.Level.INFO, "Updated savestate for {0}", player.getUuid());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
            }

            EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));
        }

        player.removeTag(TAG_EDITING);
    }

    private @NotNull TestingMapWorldNew getTestWorld() {
        if (testWorld == null) {
            testWorld = new TestingMapWorldNew(this);
        }

        return testWorld;
    }

    public void enterTestMode(@NotNull Player player) {
        Thread.startVirtualThread(() -> movePlayerToTestWorld(player));
    }

    private @Blocking void movePlayerToTestWorld(@NotNull Player player) {
        // remove from this map (leaving them in the Minestom instance)
        removePlayer(player);

        // add to the test world
        getTestWorld().acceptPlayer(player);
    }

}

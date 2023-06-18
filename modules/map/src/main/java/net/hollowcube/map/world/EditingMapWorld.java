package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.util.StringUtil;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.generation.MapGenerators;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class EditingMapWorld implements InternalMapWorld {
    private static final System.Logger logger = System.getLogger(EditingMapWorld.class.getName());

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_EDITING = Tag.Boolean("editing").defaultValue(false);

    private final MapServer server;
    private final MapData map;
    private int flags = 0;

    private final BaseWorld baseWorld;
    private TestingMapWorld testWorld = null;

    private final Set<Player> activePlayers = Collections.synchronizedSet(new HashSet<>());

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> scopedNode = EventNode.event("world-local", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_EDITING);
        }
        return true;
    });

    public EditingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.map = map;
        this.flags |= FLAG_EDITING;

        var instance = new InstanceContainer(StringUtil.seededUUID(map.getId()), DimensionTypes.FULL_BRIGHT);
        this.baseWorld = new BaseWorld(server.worldManager(), map.getId(), instance);
        instance.setGenerator(MapGenerators.voidWorld());
        instance.setTag(SELF_TAG, this);
        var eventNode = instance.eventNode();

        this.itemRegistry = new ItemRegistry();
        eventNode.addChild(itemRegistry.eventNode());
        eventNode.addChild(scopedNode);
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventSwordBreaking);
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
        this.scopedNode.addChild(eventNode);
    }

    @Override
    public @NotNull Point spawnPoint() {
        return map.getSpawnPoint();
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

        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
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
    public @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        if (activePlayers.contains(player))
            return this;
        if (testWorld != null)
            return testWorld.getMapForPlayer(player);
        return null;
    }

    @Override
    public @Blocking void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.getId(), SaveState.Type.EDITING);

        activePlayers.add(player);
        player.setTag(TAG_EDITING, true);
        player.setTag(SaveState.TAG, saveState);
        player.refreshCommands();

        var inventory = saveState.getInventory();
        player.getInventory().clear();
        for (int i = 0; i < inventory.size(); i++) {
            player.getInventory().setItemStack(i, inventory.get(i));
        }
        player.setGameMode(GameMode.CREATIVE);
        player.teleport(saveState.getPos()).join();

        player.sendMessage("Now editing " + map.getName());
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
        }

        player.removeTag(TAG_EDITING);
        activePlayers.remove(player);
    }

    private @Blocking
    @NotNull TestingMapWorld getTestWorld() {
        if (testWorld == null) {
            testWorld = new TestingMapWorld(this);
            testWorld.load();
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

    private final net.minestom.server.gamedata.tags.@Nullable Tag SWORD_TAG = MinecraftServer.getTagManager().getTag(net.minestom.server.gamedata.tags.Tag.BasicType.ITEMS, "minecraft:swords");
    private void preventSwordBreaking(PlayerBlockBreakEvent event) {
        ItemStack item = event.getPlayer().getItemInMainHand();
        if (SWORD_TAG != null && SWORD_TAG.contains(item.material().namespace())) {
            event.setCancelled(true);
        }
    }

}

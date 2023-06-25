package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.player.PlayerDataV2;
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

    private final MapInstance instance;
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

//        var instance = new InstanceContainer(StringUtil.seededUUID(map.id()), DimensionTypes.FULL_BRIGHT);
//        this.baseWorld = new BaseWorld(server.worldManager(), map.id(), instance);
        instance = new MapInstance();
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
        return map.settings().getSpawnPoint();
    }

    @Override
    public @NotNull Instance instance() {
        return instance;
    }

    @Override
    public @Blocking void load() {
        var mapData = server().mapService().getMapWorld(map.id(), true);
        if (mapData != null) {
            instance.load(mapData);
        }

        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
    }

    @Override
    public @Blocking void close() {
        logger.log(System.Logger.Level.INFO, "Closing editing world " + map.id());
        if (testWorld != null) {
            testWorld.close();
        }

        var worldData = instance.save();
        server().mapService().updateMapWorld(map.id(), worldData);

        // Unload the backing world
        instance.unload();
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
    @Blocking
    public void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());

        activePlayers.add(player);
        player.setTag(TAG_EDITING, true);
        player.setTag(SaveState.TAG, saveState);
        player.refreshCommands();

        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().clear();
        var savedItems = saveState.getInventoryItems();
        if (savedItems != null) {
            for (int i = 0; i < savedItems.size(); i++) {
                player.getInventory().setItemStack(i, savedItems.get(i));
            }
        }
        var pos = Objects.requireNonNullElse(saveState.pos(), map.settings().getSpawnPoint());
        player.teleport(pos).join();

        player.sendMessage("Now editing " + map.settings().getName());
    }

    @Override
    public @Blocking void removePlayer(@NotNull Player player) {
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            var update = new SaveStateUpdateRequest();
            update.setPos(player.getPosition());
            update.setInventoryItems(List.of(player.getInventory().getItemStacks()));

            try {
                var playerData = PlayerDataV2.fromPlayer(player);
                server.mapService().updateSaveState(map.id(), playerData.id(), saveState.id(), update);
                logger.log(System.Logger.Level.INFO, "Updated savestate for {0}", player.getUuid());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
            }
        }

        player.removeTag(TAG_EDITING);
        activePlayers.remove(player);
    }

    @Blocking
    private @NotNull TestingMapWorld getTestWorld() {
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

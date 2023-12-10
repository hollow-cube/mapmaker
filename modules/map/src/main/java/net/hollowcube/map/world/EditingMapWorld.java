package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.BlockItemPlaceEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.terraform.Terraform;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EffectPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("UnstableApiUsage")
public class EditingMapWorld implements InternalMapWorld {
    private static final System.Logger logger = System.getLogger(EditingMapWorld.class.getName());

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_EDITING = Tag.Boolean("editing").defaultValue(false);

    private static final int MAP_AUTOSAVE_INTERVAL_SEC = 60 * 5; // 5 minutes

    private final MapServer server;
    private final Terraform terraform;
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

    private final ReentrantLock saveLock = new ReentrantLock();
    private Task autoSaveTask = null;

    public EditingMapWorld(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.terraform = server.terraform();
        this.map = map;
        this.flags |= FLAG_EDITING;

        instance = new MapInstance(getDimensionName());
        instance.setGenerator(MapGenerators.voidWorld());
        instance.setTag(SELF_TAG, this);

        var eventNode = instance.eventNode();
        this.itemRegistry = new ItemRegistry();
        eventNode.addChild(itemRegistry.eventNode());
        eventNode.addChild(scopedNode);
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventSwordBreaking);
        eventNode.addListener(PlayerUseItemEvent.class, this::preventSuspiciousStew);
        eventNode.addListener(InstanceTickEvent.class, this::tick);

        eventNode.addListener(BlockItemPlaceEvent.class, event -> {
            var handler = event.getBlock().handler();
            if (!(handler instanceof ObjectBlockHandler objectHandler)) return;

            var object = objectHandler.createObjectData(event.getBlockPosition());
            var added = map.addObject(object);
            if (!added) {
                event.setCancelled(true);

                var packet = new EffectPacket(2001, event.getBlockPosition(),
                        event.getBlock().stateId(), false);
                instance.sendGroupedPacket(packet);

                event.getPlayer().sendMessage("no place");
            }

        });
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

        // Kick off autosave
        if (map.verification() == MapVerification.UNVERIFIED)
            autoSaveTask = instance.scheduler().buildTask(this::autoSaveTaskWrapper)
                    .delay(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                    .repeat(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                    .schedule();
        //todo add a property to Minestom to run async tasks in virtual threads
    }

    @Override
    public @Blocking void close(boolean shutdown) {
        logger.log(System.Logger.Level.INFO, "Closing editing world {0}", map.id());
        if (testWorld != null) {
            testWorld.close(shutdown);
        }

        var kickMessage = Component.translatable("mapmaker.shutdown");
        for (var player : Set.copyOf(activePlayers)) {
            removePlayer(player);
            if (shutdown) player.kick(kickMessage);
        }

        if (autoSaveTask != null) autoSaveTask.cancel();
        saveWorld(false);

        // Unload the backing world
        instance.unload();
    }

    @Blocking
    private void saveWorld(boolean isAutoSave) {
        saveLock.lock();
        try {
            // Save the map settings
            map.settings().withUpdateRequest(updates -> {
                try {
                    //todo map worlds should have an "owner" which is the owner if they are present, otherwise
                    // it is the first trusted player to join the world. If someone leaves it should find a new
                    // "owner" of the world, or if it cannot (only invited people left), it should close the world.
                    server().mapService().updateMap(map.owner(), map.id(), updates);
                    return true;
                } catch (Exception e) {
                    logger.log(System.Logger.Level.ERROR, "Failed to save map settings for " + map.id(), e);
                    MinecraftServer.getExceptionManager().handleException(e);
                    return false;
                }
            });

            // Save the world data (if it is unverified only)
            if (map.verification() == MapVerification.UNVERIFIED) {
                var worldData = instance.save();
                server().mapService().updateMapWorld(map.id(), worldData);
            }

            // Save the players data
            for (var player : activePlayers) {
                var playerData = PlayerDataV2.fromPlayer(player);

                var saveState = SaveState.fromPlayer(player);
                var update = updateSaveState(player, saveState);
                server().mapService().updateSaveState(map.id(), playerData.id(), saveState.id(), update);
                //todo handle errors

                // Save terraform state
                terraform.saveLocalSession(player, false);
                terraform.savePlayerSession(player, false);
                //todo handle errors here too
            }

            if (isAutoSave) {
                instance.sendMessage(Component.translatable("build.world.save.success"));
            }
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to save world " + map.id(), e);
            instance.sendMessage(Component.translatable("build.world.save.failure"));
        } finally {
            saveLock.unlock();
        }
    }

    private void autoSaveTaskWrapper() {
        Thread.startVirtualThread(() -> {
            logger.log(System.Logger.Level.INFO, "Autosaving world {0}", map.id());
            saveWorld(true);
        });
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
    public void acceptPlayer(@NotNull Player player, boolean firstSpawn) {
        var playerData = PlayerDataV2.fromPlayer(player);

        if (map.verification() == MapVerification.PENDING) {
            enterTestMode(player);
        } else {
            var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());

            activePlayers.add(player);
            player.setTag(TAG_EDITING, true);
            player.setTag(SaveState.TAG, saveState);

            MapWorldHelpers.resetPlayer(player);

            // Load Terraform State
            terraform.initPlayerSession(player, playerData.id());
            terraform.initLocalSession(player, playerData.id());

            player.setGameMode(GameMode.CREATIVE);
            saveState.setPlayStartTime(System.currentTimeMillis());

            // Read from savestate
            player.getInventory().clear();
            var savedItems = saveState.getInventoryItems();
            if (savedItems != null) {
                for (int i = 0; i < savedItems.size(); i++) {
                    player.getInventory().setItemStack(i, savedItems.get(i));
                }
            }

            player.setFlying(saveState.isFlying());
            var pos = Objects.requireNonNullElse(saveState.pos(), map.settings().getSpawnPoint());
            player.teleport(pos).join();

            if (firstSpawn)
                player.sendMessage(Component.translatable("build.world.load.first", Component.translatable(map.settings().getName())));
        }

        player.refreshCommands(); //todo this should just be done on every instance change
    }

    @Override
    public @Blocking void removePlayer(@NotNull Player player) {
        if (map.verification() == MapVerification.PENDING) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            try {

                //todo handle these errors better
                var playerData = PlayerDataV2.fromPlayer(player);

                var saveStateUpdate = updateSaveState(player, saveState);
                server.mapService().updateSaveState(map.id(), playerData.id(), saveState.id(), saveStateUpdate);

                // Save terraform state
                terraform.saveLocalSession(player, true);
                terraform.savePlayerSession(player, true);

                //todo
//                playerData.setLastEditedMap(map.id());

                logger.log(System.Logger.Level.INFO, "Updated data for {0}", player.getUuid());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
            }
        }

        player.removeTag(SaveState.TAG);
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
        getTestWorld().acceptPlayer(player, true);
    }

    private final net.minestom.server.gamedata.tags.@Nullable Tag SWORD_TAG = MinecraftServer.getTagManager().getTag(net.minestom.server.gamedata.tags.Tag.BasicType.ITEMS, "minecraft:swords");

    private void preventSwordBreaking(PlayerBlockBreakEvent event) {
        ItemStack item = event.getPlayer().getItemInMainHand();
        if (SWORD_TAG != null && SWORD_TAG.contains(item.material().namespace())) {
            event.setCancelled(true);
        }
    }

    private void preventSuspiciousStew(PlayerUseItemEvent event) {
        ItemStack itemStack = event.getItemStack();

        if (itemStack.material() == Material.SUSPICIOUS_STEW) {
            event.setCancelled(true);
        }
    }

    private @NotNull String getDimensionName() {
        return String.format("mapmaker:map/%s/e", map.id().substring(0, 8));
    }

    private @NotNull SaveStateUpdateRequest updateSaveState(@NotNull Player player, @NotNull SaveState saveState) {
        saveState.updatePlaytime();
        saveState.setPos(player.getPosition());
        saveState.setFlying(player.isFlying());
        saveState.setInventoryItems(List.of(player.getInventory().getItemStacks()));
        return saveState.getUpdateRequest();
    }

    @Override
    public @NotNull Set<Player> players() {
        return Set.copyOf(activePlayers);
    }

    public void tick(@NotNull InstanceTickEvent event) {
        var minHeight = instance.getDimensionType().getMinY() - 20;

        for (var player : activePlayers) {
            if (player.getPosition().y() < minHeight) {
                player.teleport(new Pos(map.settings().getSpawnPoint()));
            }
        }
    }
}

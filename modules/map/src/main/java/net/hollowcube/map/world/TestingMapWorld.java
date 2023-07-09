package net.hollowcube.map.world;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TestingMapWorld implements InternalMapWorld {
    private static final System.Logger logger = System.getLogger(TestingMapWorld.class.getName());

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_TESTING = Tag.Transient("testing");

    private int flags;

    private final EditingMapWorld parent;
    private final Instance instance;

    private final Set<Player> activePlayers = Collections.synchronizedSet(new HashSet<>());

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> eventNode = EventNode.event("world-local-test", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_TESTING);
        }
        return true;
    });

    TestingMapWorld(@NotNull EditingMapWorld parent) {
        this.flags |= FLAG_TESTING | FLAG_PLAYING;

        this.parent = parent;
        this.instance = parent.instance();
        // do NOT set self tag, the instance is "officially" owned by the editing world.

        this.itemRegistry = new ItemRegistry();
        this.instance.eventNode().addChild(eventNode);
    }

    @Override
    public @NotNull MapServer server() {
        return parent.server();
    }

    @Override
    public @NotNull MapData map() {
        return parent.map();
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
        this.eventNode.addChild(eventNode);
    }

    @Override
    public @NotNull Instance instance() {
        return instance;
    }

    @Override
    public @NotNull Point spawnPoint() {
        return parent.spawnPoint();
    }

    @Override
    public void load() {
        this.enabledFeatures.addAll(MapWorldHelpers.loadFeatures(this));
    }

    @Override
    public void close() {
        // Do not unregister instance, it is owned by the parent.

        // todo do we need to boot players here?
    }

    @Override
    public @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        return activePlayers.contains(player) ? this : null;
    }

    @Override
    public void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());
        System.out.println("NEW SAVE STATE " + saveState.type());

        var startingPos = player.getPosition();
        player.teleport(startingPos);
        player.refreshCommands();

        activePlayers.add(player);
        player.setTag(TAG_TESTING, true);
        player.setTag(MapHooks.PLAYING, true); // For compatibility
        player.setTag(SaveState.TAG, saveState);

        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage("Now testing " + map().settings().getName());

        EventDispatcher.call(new MapPlayerInitEvent(this, player, true));
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));

        player.removeTag(MapHooks.PLAYING);
        player.removeTag(TAG_TESTING);
        activePlayers.remove(player);

        // Save their save state if this is a pending verification
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || map().verification() != MapVerification.PENDING) return;

        saveState.getPlaytime(instance.getWorldAge()); // Triggers update
        saveState.setCompleted(true);

        try {
            var update = saveState.getUpdateRequest();

            var playerData = PlayerDataV2.fromPlayer(player);
            parent.server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
            logger.log(System.Logger.Level.INFO, "Updated testing savestate for {0}", player.getUuid());
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to save player state for {0}", player.getUuid(), e);
        }

        player.removeTag(SaveState.TAG);
    }


    public void exitTestMode(@NotNull Player player) {
        Thread.startVirtualThread(() -> movePlayerToBuildWorld(player));
    }

    private @Blocking void movePlayerToBuildWorld(@NotNull Player player) {
        // remove from this map (leaving them in the Minestom instance)
        removePlayer(player);

        // add to the test world
        parent.acceptPlayer(player);
    }

    @Override
    public @NotNull Set<Player> players() {
        return activePlayers;
    }
}

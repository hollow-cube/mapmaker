package net.hollowcube.map.world;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.event.MapWorldPlayerStartPlayingEvent;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.mapmaker.storage.SaveStateStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

class TestingMapWorldNew implements InternalMapWorldNew {
    private static final InstanceManager instanceManager = MinecraftServer.getInstanceManager();

    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_TESTING = Tag.Boolean("editing").defaultValue(false);

    private final EditingMapWorldNew parent;
    private final Instance instance;

    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> eventNode = EventNode.event("world-local-test", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_TESTING);
        }
        return true;
    });

    public TestingMapWorldNew(@NotNull EditingMapWorldNew parent) {
        this.parent = parent;
        this.instance = instanceManager.createSharedInstance((InstanceContainer) parent.instance());
        instance.setTag(SELF_TAG, this);

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
        return parent.flags() | MapWorldNew.FLAG_TESTING;
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
        // Do nothing, we are just using the parent instance.
    }

    @Override
    public void close() {
        // todo do we need to boot players here?

        instanceManager.unregisterInstance(instance);
    }

    @Override
    public void acceptPlayer(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);

        player.setTag(TAG_TESTING, true);
        player.setGameMode(GameMode.CREATIVE); //todo based on setting
        player.refreshCommands();

        var saveStateStorage = server().saveStateStorage();
        SaveState saveState;
        try {
            saveState = saveStateStorage.getLatestSaveState(playerData.getId(), map().getId(), SaveState.Type.TESTING);
        } catch (SaveStateStorage.NotFoundError e) {
            // common savestate creation logic todo move elsewhere
            saveState = new SaveState();
            saveState.setId(UUID.randomUUID().toString());
            saveState.setPlayerId(playerData.getId());
            saveState.setMapId(map().getId());
            saveState.setStartTime(Instant.now());
            saveState.setPos(map().getSpawnPoint());

            saveState.setType(SaveState.Type.TESTING);

            saveStateStorage.createSaveState(saveState);

            player.setTag(MapHooks.PLAYING, true);
            player.setTag(SaveState.TAG, saveState);
        }

        // formerly initPlayerFromSaveState
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(saveState.getPos()).join();
        player.sendMessage("Now testing " + map().getName());

        EventDispatcher.call(new MapWorldPlayerStartPlayingEvent(this, player));
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        player.removeTag(TAG_TESTING);

        //todo
    }
}

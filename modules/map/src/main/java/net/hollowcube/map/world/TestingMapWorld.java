package net.hollowcube.map.world;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.biome.BiomeContainer;
import net.hollowcube.map2.event.MapPlayerInitEvent;
import net.hollowcube.map2.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map2.util.MapWorldHelpers;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.SaveStateUpdateRequest;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class TestingMapWorld extends AbstractMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(TestingMapWorld.class);

    private final EditingMapWorld parent;

    private final EventNode<InstanceEvent> eventNode = EventNode.event("testing-node", EventFilter.INSTANCE, this::testEvent);

    public TestingMapWorld(@NotNull EditingMapWorld parent) {
        super(parent.server(), parent.map(), (MapInstance) parent.instance());
        this.parent = parent;

        parent.instance().eventNode().addChild(eventNode);
    }

    @Override
    public @NotNull Pos spawnPoint(@NotNull Player player) {
        return parent.spawnPoint(player);
    }

    @Override
    public @NotNull BiomeContainer biomes() {
        return parent.biomes(); // Always share biomes with the parent.
    }

    @NotNull
    @Override
    public EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public @NotNull EditingMapWorld buildWorld() {
        return parent;
    }

    @NonBlocking
    public void exitTestMode(@NotNull Player player) {
        FutureUtil.submitVirtual(() -> exitTestModeInternal(player));
    }

    private void exitTestModeInternal(@NotNull Player player) {
        if (!isPlaying(player) && !isSpectating(player)) {
            logger.error("Player {} tried to enter test mode for {} without being in the map. Currently in {}",
                    player.getUsername(), this, MapWorld.forPlayerOptional(player));
            return;
        }

        // remove from this map (leaving them in the Minestom instance)
        // then add them to the build world.
        removePlayer(player);
        parent.addPlayer(player);
    }

    @Override
    public void load() {
        //todo enable features
    }

    @Override
    public void close() {
        // Do not unregister instance, it is owned by the parent.

        //todo close features

        super.close();
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        // The save state for verifications needs to be created remotely, but for local testing we can create it here.
        // todo in the future verification should be done in a VerificationMapWorld or PlayingMapWorld probably.
        SaveState saveState;
        if (map().verification() == MapVerification.PENDING) {
            saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id());
        } else {
            saveState = new SaveState(
                    UUID.randomUUID().toString(), playerData.id(), map().id(),
                    SaveStateType.PLAYING
            );
        }

        super.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);

        var startingPos = player.getPosition();
        player.teleport(startingPos); //todo is this necessary it seems hella dumb?

        EventDispatcher.call(new MapPlayerInitEvent(this, player, true));
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        EventDispatcher.call(new MapWorldPlayerStopPlayingEvent(this, player));

        super.removePlayer(player);

        // Save their save state if this is a pending verification
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || map().verification() != MapVerification.PENDING) return;

        saveState.updatePlaytime();

        var update = new SaveStateUpdateRequest();
        update.setPlaytime(saveState.getPlaytime());
        update.setCompleted(saveState.isCompleted());

        try {
            var playerData = PlayerDataV2.fromPlayer(player);
            parent.server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
            logger.error("Updated testing savestate for {}", player.getUuid());
        } catch (Exception e) {
            logger.error("Failed to save player state for {}", player.getUuid(), e);
        }

        player.removeTag(SaveState.TAG);
    }

    private boolean testEvent(@NotNull InstanceEvent event) {
        if (event instanceof PlayerEvent playerEvent)
            return isPlaying(playerEvent.getPlayer());
        return true;
    }
}

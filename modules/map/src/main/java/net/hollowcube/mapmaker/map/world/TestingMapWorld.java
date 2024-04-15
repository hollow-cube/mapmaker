package net.hollowcube.mapmaker.map.world;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

@SuppressWarnings("UnstableApiUsage")
public final class TestingMapWorld extends AbstractMapMakerMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(TestingMapWorld.class);

    private final EditingMapWorld parent;

    public TestingMapWorld(@NotNull EditingMapWorld parent) {
        super(parent.server(), parent.map(), parent.features(), (MapInstance) parent.instance());
        this.parent = parent;
    }

    @Override
    public @NotNull Pos spawnPoint(@NotNull Player player) {
        return parent.spawnPoint(player);
    }

    @Override
    public @NotNull BiomeContainer biomes() {
        return parent.biomes(); // Always share biomes with the parent.
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
    public void close(@Nullable Component reason) {
        super.close(reason);

        // Override left just to comment that the instance should not be unregistered here. It is owned
        // by the parent editing world and is managed by that.
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        // The save state for verifications needs to be created remotely, but for local testing we can create it here.
        // todo in the future verification should be done in a VerificationMapWorld or PlayingMapWorld probably.
        SaveState saveState;
        if (map().verification() == MapVerification.PENDING) {
            saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id(), SaveStateType.VERIFYING);
        } else {
            // Create a fake save state, it is only used locally anyway.
            saveState = new SaveState(
                    UUID.randomUUID().toString(), playerData.id(), map().id(),
                    SaveStateType.PLAYING
            );
        }
        player.setTag(SaveState.TAG, saveState);

        super.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);

        var startingPos = player.getPosition();
        player.setTag(SPECTATOR_CHECKPOINT, startingPos);
        player.teleport(startingPos); //todo is this necessary it seems hella dumb?

        callEvent(new MapPlayerInitEvent(this, player, true));
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        callEvent(new MapWorldPlayerStopPlayingEvent(this, player));

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
}

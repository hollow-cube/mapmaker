package net.hollowcube.mapmaker.map.world;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class TestingMapWorld extends AbstractMapMakerMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(TestingMapWorld.class);

    private final EditingMapWorld parent;

    public TestingMapWorld(@NotNull EditingMapWorld parent) {
        super(parent.server(), parent.map(), (MapInstance) parent.instance());
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
    public void load() {
        features().preinitMap(this);
        
        super.load();
    }

    @Override
    public void close(@Nullable Component reason) {
        super.close(reason);

        // Override left just to comment that the instance should not be unregistered here. It is owned
        // by the parent editing world and is managed by that.
    }

    @Override
    public void preAddPlayer(@NotNull AsyncPlayerConfigurationEvent event) {
        event.getPlayer().setTag(FIRST_JOIN_TAG, true);
        // Irrelevant for testing world
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        var playerData = PlayerDataV2.fromPlayer(player);

        // Create a fake save state, it is only used locally anyway.
        var saveState = new SaveState(
                UUID.randomUUID().toString(), playerData.id(), map().id(),
                SaveStateType.PLAYING, PlayState.SERIALIZER, new PlayState()
        );
        player.setTag(SaveState.TAG, saveState);

        super.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);

        var startingPos = player.getPosition();
        SpectateHandler.setCheckpoint(player, startingPos);
        player.teleport(startingPos, Vec.ZERO, null, RelativeFlags.NONE); //todo is this necessary it seems hella dumb?

        var isMapJoin = player.getAndSetTag(FIRST_JOIN_TAG, null) != null;
        callEvent(new MapPlayerInitEvent(this, player, true, isMapJoin));
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        // This must be done at end of tick to avoid trying to perform
        // play-state actions after we have removed their play state.
        FutureUtil.waitForEndOfTick(player, () -> {
            callEvent(new MapWorldPlayerStopPlayingEvent(this, player));

            super.removePlayer(player);

            // We never need to save their state because this is a test state and only managed locally.
            player.removeTag(SaveState.TAG);
        });
    }

    @Override
    protected void sendBossBars(@NotNull Player player) {
        // Intentionally do nothing so we preserve the editing boss bar
    }
}

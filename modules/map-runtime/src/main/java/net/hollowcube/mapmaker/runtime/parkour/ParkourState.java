package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.util.MapCompletionAnimation;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.runtime.parkour.hud.*;
import net.hollowcube.mapmaker.runtime.parkour.item.*;
import net.hollowcube.mapmaker.runtime.parkour.setting.OnlySprintSetting;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public sealed interface ParkourState extends PlayerState<ParkourState, ParkourMapWorld> {

    sealed interface AnyPlaying extends ParkourState {
        AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 500, AttributeOperation.ADD_VALUE);

        SaveState saveState();

        AnyPlaying withSaveState(SaveState saveState);

        default boolean isScorable() {
            return false;
        }

        @Override
        default void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).addModifier(NO_FALL_DAMAGE_MODIFIER);
            player.updateViewerRule(new PlayerVisibility.ViewerRule(player, world));
            ((MapPlayer) player).setVisibilityFunc(new PlayerVisibility.VisibilityRule(player, world));

            // Timer is only added for scorable playing states.
            ActionBar.forPlayer(player).addProvider(ParkourTimerHud.INSTANCE);

            final var playState = saveState().state(PlayState.class);

            boolean isFreshState = saveState().getPlaytime() == 0, isMapJoin = lastState == null;

            // Attempt to add base effect data if this is a fresh save state.
            var spawnCheckpoint = world.getTag(ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS);
            if (spawnCheckpoint != null && isFreshState) spawnCheckpoint.actions().applyTo(player, playState);

            world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState(), playState, isFreshState, isMapJoin, false));

            if (!isFreshState) {
                // If the playtime is non-zero (ie they have played before) start timing immediately.
                // Otherwise, we will start timing when they move the first time.
                saveState().setPlayStartTime(System.currentTimeMillis());
            } else ((MapPlayer) player).resetTouchingState();

            // If we are not exiting config for the first time, spawn at the save position.
            // For initial exit we use the player respawn point set during config, so this
            // logic also exists in ParkourMapWorld.
            if (lastState != null) {
                resetTeleport(player, Objects.requireNonNullElseGet(playState.pos(),
                        () -> world.map().settings().getSpawnPoint()));
            }

            if (lastState == null && MapFeatureFlags.DEBUG_PLAYING_OVERLAY.test(player)) {
                ActionBar.forPlayer(player).addProvider(ParkourDebugHud.INSTANCE);
            }
        }

        @Override
        default void resetPlayer(ParkourMapWorld world, Player player, @Nullable ParkourState nextState) {
            player.updateViewerRule(null);

            // Any time we switch away from playing we attempt to save the current state.
            var saveState = saveState();
            saveState.updatePlaytime();
            saveState.setPlayStartTime(0);
            var playState = saveState.state(PlayState.class);
            if (!OnlySprintSetting.canSprint(world, playState)) {
                // For only sprint maps, the "current state" when leaving is always actually the last checkpoint.
                // If there is no last state then we reset the playtime & start state of the save state as well.
                if (playState.lastState() != null) {
                    playState = playState.lastState();
                    playState.setLastState(playState.copy());
                    saveState.setState(playState);
                } else {
                    saveState.setState(playState = new PlayState());
                    saveState.setPlaytime(0);
                    saveState.setPlayStartTime(0);
                }
            } else {
                // Otherwise set the current pos and update their state.
                playState.setPos(player.getPosition());
                world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
            }

            ((MapPlayer) player).removeOwnedEntities();
            ((MapPlayer) player).resetTouchingState();

            // The following is deinit logic which should not happen when switching from play to play (aka checkpoint reset).
            if (nextState instanceof AnyPlaying) return;

            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).removeModifier(NO_FALL_DAMAGE_MODIFIER);
            ActionBar.forPlayer(player).removeProvider(ParkourTimerHud.INSTANCE);

            if (nextState != null || player.isRemoved()) return;

            var emptyPlayState = new PlayState();
            world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player,
                    saveState.copy(emptyPlayState), emptyPlayState,
                    false, false, true));
            GhostBlockHolder.clear(player, true);
            ResetHeightDisplay.clear(player);
        }

        static CompletableFuture<Void> resetTeleport(Player player, Pos position) {
            try {
                player.sendPacket(new BundlePacket());

                var chunk = player.getInstance().getChunkAt(position);
                if (chunk != null) player.sendPacket(chunk.getFullDataPacket());
                var ghostBlocks = GhostBlockHolder.forPlayerOptional(player);
                if (ghostBlocks != null) ghostBlocks.load(ghostBlocks.save());

                player.setFlyingWithElytra(false);
                var future = player.teleport(position, Vec.ZERO, null, RelativeFlags.NONE);

                // Force the player immediately into whatever pose the server thinks they should be in at the target pos.
                if (player instanceof MapPlayer mp) mp.updatePose();
                player.sendPacket(new EntityMetaDataPacket(player.getEntityId(),
                        Map.of(MetadataDef.Player.POSE.index(), Metadata.Pose(player.getPose()))));

                return future;
            } finally {
                player.sendPacket(new BundlePacket());
            }
        }
    }

    /// @param saveState The current save state for this playing state.
    record Playing2(SaveState saveState) implements AnyPlaying {

        @Override
        public boolean isScorable() {
            return true;
        }

        @Override
        public AnyPlaying withSaveState(SaveState saveState) {
            return new Playing2(saveState);
        }

        @Override
        public void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            MapWorldHelpers.resetPlayerOnTickThread(player);
            AnyPlaying.super.configurePlayer(world, player, lastState);

            world.itemRegistry().setItemStack(player, ReturnToCheckpointItem.ID, 0);
            if (!world.map().getSetting(MapSettings.NO_SPECTATOR))
                world.itemRegistry().setItemStack(player, ToggleSpectatorModeItem.ID_ON, 1);
            world.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            boolean isFreshState = saveState().getPlaytime() == 0;
            if (!isFreshState) {
                // Additionally, if we are entering play mode on an old save-state set
                // the touching state immediately because we should already be inside.
                ((MapPlayer) player).updateTouchingState(world, false);
            }
        }

        @Override
        public void resetPlayer(ParkourMapWorld world, Player player, @Nullable ParkourState nextState) {
            AnyPlaying.super.resetPlayer(world, player, nextState);

            // Wdon't save if entering finished state, that state will handle saving the record.
            if (!(nextState instanceof Finished))
                FutureUtil.submitVirtual(() -> writeSaveState(world, player, saveState));
        }

    }

    /// @param saveState The temporary save state for this testing session
    /// @param parent    The owning spec state, if this is spec + gameplay settings. May be null if other form of testing.
    record Testing(SaveState saveState, @Nullable Spectating parent) implements AnyPlaying {

        @Override
        public AnyPlaying withSaveState(SaveState saveState) {
            return new Testing(saveState, parent);
        }

        @Override
        public void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            ActionBar.forPlayer(player).removeProvider(SpectatorModeHud.INSTANCE);
            MapWorldHelpers.resetPlayerOnTickThread(player, false);
            AnyPlaying.super.configurePlayer(world, player, lastState);

            ((MapPlayer) player).resetTouchingState();
            ((MapPlayer) player).updateTouchingState(world, true);
        }
    }

    /// @param savedState The save state from when the player was last playing.
    /// @param gameState  The gameplay settings state for this spectate session.
    record Spectating(SaveState savedState, PlayState gameState) implements ParkourState {

        public Spectating(SaveState savedState) {
            // Initially empty, see note in configurePlayer.
            this(savedState, new PlayState());
        }

        @Override
        public void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            ActionBar.forPlayer(player).addProvider(SpectatorModeHud.INSTANCE);
            player.setAllowFlying(gameState.get(SpectateHelper.SPECTATOR_FLIGHT, true));

            world.itemRegistry().setItemStack(player, ReturnToCheckpointItem.ID, 0);
            world.itemRegistry().setItemStack(player, ToggleSpectatorModeItem.ID_OFF, 1);
            world.itemRegistry().setItemStack(player, SetSpectatorCheckpointItem.ID, 2);
            if (MapFeatureFlags.SPEC_GAMEPLAY_SETTINGS.test(player)) {
                world.itemRegistry().setItemStack(player, ToggleGameplayItem.ID_ON, 7);
            } else {
                world.itemRegistry().setItemStack(player, ToggleFlightItem.ID_OFF, 7);
            }
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            // We encounter somewhat of a strange issue here - We create the Spectating instance part of the way
            // through a tick but notably _before_ resetPlayer has been called on lastState which means that
            // the playstate is not up-to-date at the time of save.
            // There is perhaps a better way to handle this, but for now we create the spec state with an empty
            // play state and mutate it once here.
            if (!gameState.get(SpectateHelper.GAME_STATE_SAVED, false)) {
                var saveState = lastState instanceof AnyPlaying p ? p.saveState() : savedState;
                gameState.setFrom(saveState.state(PlayState.class));
                gameState.set(SpectateHelper.GAME_STATE_SAVED, true);
            }
        }

        @Override
        public void resetPlayer(ParkourMapWorld world, Player player, @Nullable ParkourState nextState) {
            // Don't remove the spectating hud if we are switching to a non-scorable play state
            if (nextState instanceof Playing2 || nextState == null) {
                ActionBar.forPlayer(player).removeProvider(SpectatorModeHud.INSTANCE);
            }
        }
    }

    record Finished(SaveState saveState, long timestamp) implements ParkourState {

        @Override
        public void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            ActionBar.forPlayer(player).addProvider(FinishedModeHud.INSTANCE);
            player.setAllowFlying(true);

            world.itemRegistry().setItemStack(player, RateMapItem.ID, 0);
            world.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            saveState.complete(timestamp);
            FutureUtil.submitVirtual(() -> writeSaveState(world, player, saveState));

            // If this is a verification, immediately remove them from the world and send them back to the hub
            if (world.map().verification() == MapVerification.PENDING) {
                FutureUtil.submitVirtual(() -> world.server().bridge().joinHub(player));
                return;
            }

            // This is delegated back to the world kinda stupidly. We need to be able
            // to override the behavior for testing worlds
            world.performFinishEffects(player, saveState);
        }

        @Override
        public void resetPlayer(ParkourMapWorld world, Player player, @Nullable ParkourState nextState) {
            ActionBar.forPlayer(player).removeProvider(FinishedModeHud.INSTANCE);

            // In case animation hasn't completed yet, cancel it.
            MapCompletionAnimation.cancel(player);
        }

    }

    @Override
    default ParkourState handleConflict(ParkourState other) {
        // Finishing never has priority
        if (other instanceof Finished) return this;
        if (this instanceof Finished) return other;

        // Spec always has priority because it is always a user input
        // The theory here is that if you enter spec any action that happened on that tick will happen
        // as soon as you leave spec anyway.
        if (other instanceof Spectating) return other;
        if (this instanceof Spectating) return this;

        // DNC between testing and return to checkpoint and full reset
        return other;
    }

    private static void writeSaveState(ParkourMapWorld world, Player player, SaveState saveState) {
        var update = saveState.createUpsertRequest();
        update.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));

        // Write the save state to the database
        try {
            var playerData = PlayerData.fromPlayer(player);
            world.server().mapService().updateSaveState(
                    world.map().id(), playerData.id(), saveState.id(), update);
        } catch (Exception e) {
            var wrappedException = new RuntimeException("failed to save player save state", e);
            ExceptionReporter.reportException(wrappedException, player);
        }
    }

}

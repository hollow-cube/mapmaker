package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.util.MapCompletionAnimation;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.runtime.parkour.hud.*;
import net.hollowcube.mapmaker.runtime.parkour.item.*;
import net.hollowcube.mapmaker.runtime.parkour.setting.OnlySprintSetting;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.kyori.adventure.text.Component;
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

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;


public sealed interface ParkourState extends PlayerState<ParkourState, ParkourMapWorld> {

    sealed interface AnyPlaying extends ParkourState {
        AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 1024, AttributeOperation.ADD_MULTIPLIED_BASE);
        Instant NO_POSE_CHANGES_EPOCH = Instant.ofEpochMilli(1756771200000L); // 2025-09-02 12:00:00 GMT

        SaveState saveState();

        AnyPlaying withSaveState(SaveState saveState);

        default boolean isScorable() {
            return false;
        }

        @Override
        default void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            var mp = (MapPlayer) player;

            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).addModifier(NO_FALL_DAMAGE_MODIFIER);
            player.updateViewerRule(new PlayerVisibility.ViewerRule(player, world));
            mp.setVisibilityFunc(new PlayerVisibility.VisibilityRule(player, world));

            var noxesium = NoxesiumPlayer.get(player);
            noxesium.clear();
            noxesium.set(NoxesiumGameComponents.DISABLE_SPIN_ATTACK_COLLISIONS, true);
            noxesium.set(NoxesiumGameComponents.CLIENT_AUTHORITATIVE_ELYTRA, true);

            // Timer is only added for scorable playing states.
            ActionBar.forPlayer(player).addProvider(ParkourTimerHud.INSTANCE);

            final var playState = saveState().state(PlayState.class);

            boolean isFreshState = saveState().getPlaytime() == 0, isMapJoin = lastState == null;

            // Attempt to add base effect data if this is a fresh save state.
            boolean hasAppliedSpawnActions = playState.get(Attachments.START_ACTIONS_APPLIED, false);
            var spawnCheckpoint = world.getTag(ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS);
            if (spawnCheckpoint != null && !hasAppliedSpawnActions) {
                spawnCheckpoint.actions().applyTo(player, playState);
                playState.set(Attachments.START_ACTIONS_APPLIED, true);
            }

            // If we are not exiting config for the first time, spawn at the save position.
            // For initial exit we use the player respawn point set during config, so this
            // logic also exists in ParkourMapWorld.
            if (lastState != null) {
                resetTeleport(player, Objects.requireNonNullElseGet(playState.pos(),
                        () -> world.map().settings().getSpawnPoint()));
            }

            world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState(), playState, isFreshState, isMapJoin, false));

            if (!isFreshState) {
                // If the playtime is non-zero (ie they have played before) start timing immediately.
                // Otherwise, we will start timing when they move the first time.
                saveState().setPlayStartTime(System.nanoTime() / 1_000_000);
                saveState().setStartLatency(mp.averageLatency());
            } else ((MapPlayer) player).resetTouchingState();

            var scriptContext = world.scriptContext();
            if (scriptContext != null) scriptContext.initializePlayer((MapPlayer) player);

            if (lastState == null && MapFeatureFlags.DEBUG_PLAYING_OVERLAY.test(player)) {
                ActionBar.forPlayer(player).addProvider(ParkourDebugHud.INSTANCE);
            }

            var map = world.map();
            switch (map.getSetting(MapSettings.CAN_SEND_POSE)) {
                case NOT_SET ->
                        mp.setCanSendPose(map.publishedAt() != null && map.publishedAt().isBefore(NO_POSE_CHANGES_EPOCH));
                case TRUE -> mp.setCanSendPose(true);
                case FALSE -> mp.setCanSendPose(false);
            }
        }

        @Override
        default void resetPlayer(ParkourMapWorld world, Player player, @Nullable ParkourState nextState) {
            var scriptContext = world.scriptContext();
            if (scriptContext != null) scriptContext.destroyPlayer((MapPlayer) player);

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

            ((MapPlayer) player).setCooldowns(playState.lastState().cooldownGroups());

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
                if (chunk != null) {
                    player.sendPacket(chunk.getFullDataPacket());
                    var ghostBlocks = GhostBlockHolder.forPlayerOptional(player);
                    if (ghostBlocks != null) ghostBlocks.resendChunk(chunk);
                }

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
            if (SpectateHelper.canSpectate(world, player))
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
            boolean shouldSave = !(nextState instanceof Finished)
                    // Save if exiting, entering spec, >10s playing, or completed
                    && (nextState == null || nextState instanceof Spectating || saveState.getRealPlaytime() > 10_000 || saveState.isCompleted());
            if (shouldSave) FutureUtil.submitVirtual(() -> writeSaveState(world, player, saveState));
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
            world.itemRegistry().setItemStack(player, ToggleGameplayItem.ID_ON, 7);
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            // We encounter somewhat of a strange issue here - We create the Spectating instance part of the way
            // through a tick but notably _before_ resetPlayer has been called on lastState which means that
            // the playstate is not up-to-date at the time of save.
            // There is perhaps a better way to handle this, but for now we create the spec state with an empty
            // play state and mutate it once here.
            if (!gameState.get(SpectateHelper.GAME_STATE_SAVED, false)) {
                var saveState = lastState instanceof AnyPlaying p ? p.saveState() : savedState;
                gameState.setFrom(saveState.state(PlayState.class));

                // If we have an empty save state with no playtime, try to add world spawn effects.
                boolean hasAppliedSpawnActions = gameState.get(Attachments.START_ACTIONS_APPLIED, false);
                var spawnCheckpoint = world.getTag(ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS);
                if (spawnCheckpoint != null && !hasAppliedSpawnActions) {
                    spawnCheckpoint.actions().applyTo(player, gameState);
                    gameState.set(Attachments.START_ACTIONS_APPLIED, true);
                }

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

    record Finished(SaveState saveState) implements ParkourState {

        @Override
        public void configurePlayer(ParkourMapWorld world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            ActionBar.forPlayer(player).addProvider(FinishedModeHud.INSTANCE);
            player.setAllowFlying(true);

            world.itemRegistry().setItemStack(player, RateMapItem.ID, 0);
            world.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            FutureUtil.submitVirtual(() -> writeSaveState(world, player, saveState));

            // If this is a verification, immediately remove them from the world and send them back to the hub
            if (world.map().verification() == MapVerification.PENDING) {
                player.sendMessage(Component.translatable(
                    "map.completed.first",
                    Component.text(formatMapPlaytime(saveState.getEffectivePlaytime(), true))
                ));

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

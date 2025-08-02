package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapPlayer2;
import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.feature.play.MapCompletionAnimation;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStartPlayingEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.runtime.parkour.hud.ParkourTimerHud;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ReturnToCheckpointItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleGameplayItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleSpectatorModeItem;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.hollowcube.mapmaker.map.feature.play.BaseParkourMapFeatureProvider.SPAWN_CHECKPOINT_EFFECTS;


public sealed interface ParkourState extends PlayerState<ParkourState, ParkourMapWorld2> {

    record Playing(SaveState saveState, boolean isScorable) implements ParkourState {
        private static final AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 500, AttributeOperation.ADD_VALUE);

        @Override
        public void configurePlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).addModifier(NO_FALL_DAMAGE_MODIFIER);
            ActionBar.forPlayer(player).addProvider(ParkourTimerHud.INSTANCE);

            // 123 = items
            world.itemRegistry().setItemStack(player, ReturnToCheckpointItem.ID, 4);
            world.itemRegistry().setItemStack(player, ToggleSpectatorModeItem.ID_ON, 5);
            world.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
            // todo below is temp, remove it. in reality we should not probably init hotbar when coming from spec & non scorable.
            if (!isScorable) world.itemRegistry().setItemStack(player, ToggleGameplayItem.ID_OFF, 7);
            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 8);

            final var playState = saveState.state(PlayState.class);

            // If we are not exiting config for the first time, spawn at the save position.
            // For initial exit we use the player respawn point set during config, so this
            // logic also exists in ParkourMapWorld.
            if (lastState != null) {
                resetTeleport(player, Objects.requireNonNullElseGet(playState.pos(),
                        () -> world.map().settings().getSpawnPoint()));
            }

            // todo our isFreshState logic here is kinda bad since we now save the empty state and would
            //      reuse it on next join. I think in theory if we had an ADD_TIME action it would add
            //      repeatedly if you leave and join
            //      Should write a test and fix

            boolean isFreshState = saveState.getPlaytime() == 0, isMapJoin = lastState == null;

            // Attempt to add base effect data if this is a fresh save state.
            var spawnCheckpoint = world.getTag(SPAWN_CHECKPOINT_EFFECTS);
            if (spawnCheckpoint != null && isFreshState) spawnCheckpoint.actions().applyTo(player, playState);

            // todo not sure we should be calling startPlayingEvent when returning to checkpoint, which currently could happen.
            world.callEvent(new ParkourMapPlayerStartPlayingEvent(world, player, isFreshState, isMapJoin));
            world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, playState, isFreshState));

            // If the playtime is non-zero (ie they have played before) start timing immediately.
            // Otherwise, we will start timing when they move the first time.
            if (saveState.getPlaytime() > 0) saveState.setPlayStartTime(System.currentTimeMillis());
        }

        @Override
        public void resetPlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState nextState) {

            if (!(nextState instanceof Playing)) {
                // The following is deinit logic which should not happen when switching from play to play (aka checkpoint).

                player.getAttribute(Attribute.SAFE_FALL_DISTANCE).removeModifier(NO_FALL_DAMAGE_MODIFIER);
                ActionBar.forPlayer(player).removeProvider(ParkourTimerHud.INSTANCE);
                ((MapPlayer2) player).removeOwnedEntities();
            }

            // Any time we switch away from playing we attempt to save the current state.
            saveState.updatePlaytime();
            saveState.setPlayStartTime(0);
            var playState = saveState.state(PlayState.class);
            playState.setPos(player.getPosition());

            if (isScorable) FutureUtil.submitVirtual(() -> {
                var update = saveState.createUpsertRequest();
                update.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));

                // Write the save state to the database
                try {
                    var playerData = PlayerDataV2.fromPlayer(player);
                    var resp = world.server().mapService().updateSaveState(
                            world.map().id(), playerData.id(), saveState.id(), update);
                    // todo use the response it has relevant info
                } catch (Exception e) {
                    var wrappedException = new RuntimeException("failed to save player save state", e);
                    ExceptionReporter.reportException(wrappedException, player);
                }
            });
        }

        static CompletableFuture<Void> resetTeleport(Player player, Pos position) {
            player.setFlyingWithElytra(false);
            return player.teleport(position, Vec.ZERO, null, RelativeFlags.NONE);
        }
    }

    record Spectating(PlayState savedPlayState, boolean finished) implements ParkourState {

        @Override
        public void configurePlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            player.setAllowFlying(true);

            world.itemRegistry().setItemStack(player, ToggleSpectatorModeItem.ID_OFF, 5);
            world.itemRegistry().setItemStack(player, ToggleGameplayItem.ID_ON, 7);
        }

        @Override
        public void resetPlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState nextState) {
            // In case animation hasn't completed yet, cancel it.
            if (finished) MapCompletionAnimation.cancel(player);
        }

    }

}

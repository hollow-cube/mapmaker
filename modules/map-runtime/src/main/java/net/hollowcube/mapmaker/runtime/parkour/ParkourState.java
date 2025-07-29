package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.feature.play.MapCompletionAnimation;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStartPlayingEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleGameplayItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleSpectatorModeItem;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public sealed interface ParkourState extends PlayerState<ParkourState, ParkourMapWorld2> {

    record Playing(SaveState saveState, boolean isScorable) implements ParkourState {
        private static final AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 500, AttributeOperation.ADD_VALUE);

        @Override
        public void configurePlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState lastState) {
            ParkourState.super.configurePlayer(world, player, lastState);
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).addModifier(NO_FALL_DAMAGE_MODIFIER);

            // 123 = items
            // 5 = cp item
            world.itemRegistry().setItemStack(player, ToggleSpectatorModeItem.ID_ON, 5);
            world.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
            // todo below is temp, remove it
            if (!isScorable) world.itemRegistry().setItemStack(player, ToggleGameplayItem.ID_OFF, 7);
            // 9 = details

            // If we are not exiting config for the first time, spawn at the save position.
            if (lastState != null) {
                final var playState = saveState.state(PlayState.class);
                resetTeleport(player, Objects.requireNonNullElseGet(playState.pos(),
                        () -> world.map().settings().getSpawnPoint()));
            }

            boolean isFreshState = saveState.getPlaytime() == 0, isMapJoin = lastState == null;
            world.callEvent(new ParkourMapPlayerStartPlayingEvent(world, player, isFreshState, isMapJoin));

            // todo
            world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, saveState.state(PlayState.class)));
        }

        @Override
        public void resetPlayer(ParkourMapWorld2 world, Player player, @Nullable ParkourState nextState) {
            player.getAttribute(Attribute.SAFE_FALL_DISTANCE).removeModifier(NO_FALL_DAMAGE_MODIFIER);

            // Any time we switch away from playing we attempt to save the current state.
            saveState.updatePlaytime();
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
            FireworkRocketItem.removeRocket(player);
            player.getPlayerMeta().setFlyingWithElytra(false);
            player.getInstance().eventNode().call(new PlayerStopFlyingWithElytraEvent(player));
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

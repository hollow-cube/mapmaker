package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.event.MapPlayerStartFinishedEvent;
import net.hollowcube.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.feature.play.effect.BaseEffectData;
import net.hollowcube.map.feature.play.item.*;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static net.hollowcube.mapmaker.feature.FeatureFlag.player;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/parkour", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerStartSpectatorEvent.class, this::initSpectatorPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer)
            .addListener(MapPlayerStartFinishedEvent.class, this::initFinishedPlayer)

            .addListener(MapPlayerCheckpointChangeEvent.class, this::handleCheckpointChange)
            .addListener(MapPlayerStatusChangeEvent.class, this::handleStatusChange)
            .addListener(InstanceTickEvent.class, this::handleTick);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_PLAYING) == 0)
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.addScopedEventNode(eventNode);

        var itemRegistry = world.itemRegistry();
        itemRegistry.registerSilent(MapDetailsItem.INSTANCE);
        itemRegistry.registerSilent(ReturnToCheckpointItem.INSTANCE);
        itemRegistry.registerSilent(EnterSpectatorModeItem.INSTANCE);
        itemRegistry.registerSilent(ExitSpectatorModeItem.INSTANCE);
        itemRegistry.registerSilent(ResetSaveStateItem.INSTANCE);
        itemRegistry.registerSilent(ReturnToHubItem.INSTANCE);
        itemRegistry.registerSilent(ExitTestModeItem.INSTANCE);
        itemRegistry.registerSilent(ReturnToSpectatorCheckpointItem.INSTANCE);
        itemRegistry.registerSilent(SetSpectatorCheckpointItem.INSTANCE_SPECTATOR);
        itemRegistry.registerSilent(SetSpectatorCheckpointItem.INSTANCE_TESTING);
        itemRegistry.registerSilent(ToggleFlightItem.INSTANCE);
        itemRegistry.registerSilent(RateMapItem.INSTANCE);

        // Controls player visibility
        world.instance().scheduler()
                .buildTask(() -> updateViewership(world))
                .repeat(TaskSchedule.tick(5))
                .schedule();

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if ((event.getMapWorld().flags() & MapWorld.FLAG_TESTING) != 0) {
            inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
            inventory.setItemStack(1, itemRegistry.getItemStack(ReturnToCheckpointItem.ID, null));
            inventory.setItemStack(2, itemRegistry.getItemStack(SetSpectatorCheckpointItem.ID_TESTING, null));

            inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ExitTestModeItem.ID, null));
        } else {
            inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
            inventory.setItemStack(1, itemRegistry.getItemStack(ReturnToCheckpointItem.ID, null));
            if (MapFeatureFlags.RATE_MAP.test(player(player)) && MapRatingFeatureProvider.isMapRatable(event.mapWorld())) {
                inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));
            }

            inventory.setItemStack(4, itemRegistry.getItemStack(EnterSpectatorModeItem.ID, null));

            inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));
        }

        if (event.isFirstInit()) {
            player.updateViewableRule(p -> {
                if (p.isInvisible()) return true;
                return player.getDistanceSquared(p) > 3.5 * 3.5;
            });

            //todo this should happen async i guess
//            var authorName = event.getMapWorld().server().playerService().getPlayerDisplayName2(event.mapWorld().map().owner()).build();
//            player.showTitle(Title.title(
//                    Component.text(event.mapWorld().map().settings().getName()),
//                    Component.text("by ", TextColor.color(0xCCCCCC)).append(authorName),
//                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
//            ));
        }
    }

    public void initSpectatorPlayer(@NotNull MapPlayerStartSpectatorEvent event) {
        var player = event.getPlayer();

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
        inventory.setItemStack(1, itemRegistry.getItemStack(ReturnToSpectatorCheckpointItem.ID, null));
        inventory.setItemStack(2, itemRegistry.getItemStack(SetSpectatorCheckpointItem.ID_SPECTATOR, null));
        inventory.setItemStack(4, itemRegistry.getItemStack(ExitSpectatorModeItem.ID, null));
        inventory.setItemStack(7, itemRegistry.getItemStack(ToggleFlightItem.ID, null));
        inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));

    }

    public void initFinishedPlayer(@NotNull MapPlayerStartFinishedEvent event) {
        var player = event.getPlayer();

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
        if (MapFeatureFlags.RATE_MAP.test(player(player)) && MapRatingFeatureProvider.isMapRatable(event.mapWorld())) {
            inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));
        }

        inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
        inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));

    }

    public void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        player.updateViewableRule((p) -> true);
    }

    /**
     * Resets the player to the start of the map.
     *
     * @param player    the player to reset
     * @param saveState the save state of the player
     */
    public void hardReset(@NotNull Player player, @NotNull SaveState saveState) {
        // Remove the playing tag so they can't trigger a checkpoint/status/completion
        player.removeTag(MapHooks.PLAYING);

        var newPlayState = new SaveState.PlayState();
        saveState.setPlayState(newPlayState);

        var world = MapWorld.forPlayer(player);
        player.teleport(world.map().settings().getSpawnPoint()).thenRun(() -> {
            updatePlayerFromState(player, newPlayState);
            player.setTag(MapHooks.PLAYING, true);
        });
    }

    /**
     * Resets the player to their last checkpoint, or to the start of the map if they don't have a checkpoint.
     *
     * @param player the player to reset
     */
    public void softReset(@NotNull Player player, @NotNull SaveState saveState) {
        var playState = saveState.playState();
        // If they don't have a checkpoint or are on their last life, do a hard reset.
        if (playState.lastState().isEmpty() || playState.lives().orElse(0) == 1) {
            hardReset(player, saveState);
            return;
        }

        // Remove the playing tag so they can't trigger a checkpoint/status/completion
        player.removeTag(MapHooks.PLAYING);

        // "pop" the last state to the current
        playState = playState.lastState().get();
        if (playState.lives().isPresent())
            // This is definitely valid, we checked above to see if this was the last life.
            playState.setLives(playState.lives().get() - 1);
        saveState.setPlayState(playState);
        // Create a copy so that we can reset to the checkpoint again
        playState.setLastState(playState.copy());

        // Apply the current state to the player and teleport them
        updatePlayerFromState(player, playState);
        player.teleport(playState.pos().orElseThrow()).thenRun(() -> player.setTag(MapHooks.PLAYING, true));
    }

    public void handleCheckpointChange(@NotNull MapPlayerCheckpointChangeEvent event) {
        var player = event.getPlayer();
        var state = SaveState.fromPlayer(player).playState();
        var data = event.effectData();

        // Ensure the event should trigger a checkpoint change for the current players state
        if (data.progressIndex() > 0 && state.progressIndex().orElse(-1) >= data.progressIndex())
            // todo check this logic not sure its sound. Pretty sure we should allow the same progress index if its repeatable
            return; // Player has already passed this progress index.
        if (state.lastState().isPresent() && state.lastState().get().hasStatus(event.checkpointId()))
            return; // Player already has this checkpoint in their history (they are backtracking)

        // The checkpoint (reset) pos is set to the teleport if its present, or the first
        // position the player touched the checkpoint otherwise. todo probably need to do a gravity snap here
        // to bring it down to the ground.
        var checkpointPos = data.teleport().orElse(player.getPosition());

        // Apply the checkpoint/effect changes
        data.setTimeLimit(-1); // Time always reset on checkpoint
        updateBaseEffectState(player, data, state);
        state.setMaxLives(data.lives());
        state.setLives(data.lives());

        // Cache the last state so that we can reset back here.
        state.setLastState(new SaveState.PlayState(
                Optional.empty(),
                // Set the history to only contain the single checkpoint id so that you can go back to a previous
                // checkpoint though of course this can be prevented by using progress indices
                List.of(event.checkpointId()),
                state.progressIndex(),
                state.timeLimit(),
                state.resetHeight(),
                state.potionEffects(),
                Optional.of(checkpointPos),
                state.maxLives(),
                state.lives()
        ));

        // Update the player based on the new state
        updatePlayerFromState(player, state);
    }

    public void handleStatusChange(@NotNull MapPlayerStatusChangeEvent event) {
        var player = event.getPlayer();
        var state = SaveState.fromPlayer(player).playState();
        var data = event.effectData();

        // Ensure the event should trigger a status change for the current players state
        if (!data.repeatable() && state.hasStatus(event.statusId()))
            return; // Player already has the status plate in this checkpoint.
        if (data.progressIndex() > 0 && state.progressIndex().orElse(-1) >= data.progressIndex())
            // todo check this logic not sure its sound. Pretty sure we should allow the same progress index if its repeatable
            return; // Player has already passed this progress index.

        // Apply the status changes
        updateBaseEffectState(player, data, state);
        if (data.extraTime() > 0 && state.timeLimit().isPresent()) {
            state.setTimeLimit(state.timeLimit().get() + data.extraTime());
        }
        state.addStatus(event.statusId());

        // Update the player based on the new state
        updatePlayerFromState(player, state);
    }

    public void handleTick(@NotNull InstanceTickEvent event) {
        var instance = event.getInstance();

        var players = instance.getEntityTracker().entities(EntityTracker.Target.PLAYERS);
        for (var player : players) {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) continue;

            var resetHeight = saveState.playState().resetHeight().orElse(-64);
            if (player.getPosition().y() < resetHeight) {
                softReset(player, saveState);
            }
        }
    }

    private void updateBaseEffectState(@NotNull Player player, @NotNull BaseEffectData data, @NotNull SaveState.PlayState state) {
        if (data.progressIndex() != -1) {
            state.setProgressIndex(data.progressIndex());
        }
        if (data.timeLimit() != -1) {
            // Only update the time limit if it is assigned in this effect.
            // In a checkpoint it will have been reset prior to calling this function.
            state.setTimeLimit(data.timeLimit());
        }
        if (data.resetHeight() != BaseEffectData.NO_RESET_HEIGHT) {
            state.setResetHeight(data.resetHeight());
        }
        if (data.clearPotionEffects()) {
            //todo
        }
        if (!data.potionEffects().isEmpty()) {
            //todo
        }
        if (data.teleport().isPresent()) {
            player.teleport(data.teleport().get()); //todo teleport is not immediate, need to handle that.
        }
    }

    private void updatePlayerFromState(@NotNull Player player, @NotNull SaveState.PlayState state) {
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2 * state.maxLives().orElse(10));
        player.setHealth(2 * state.lives().orElse(10));
    }

    private void updateViewership(@NotNull MapWorld world) {
        for (Player p : world.players()) {
            p.updateViewableRule();
        }
    }

}

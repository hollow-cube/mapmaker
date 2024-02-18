package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.feature.play.effect.BaseEffectData;
import net.hollowcube.map.feature.play.item.*;
import net.hollowcube.map.util.MapMessages;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.map2.AbstractMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.event.MapPlayerInitEvent;
import net.hollowcube.map2.event.MapPlayerStartFinishedEvent;
import net.hollowcube.map2.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.map2.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.potion.Potion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;
import static net.hollowcube.mapmaker.feature.FeatureFlag.player;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {
    // This tag is present when the player has an active countdown and holds the time at which
    // the countdown will end, in ms since epoch.
    public static final Tag<Long> COUNTDOWN_END = Tag.Long("mapmaker:play/countdown_end").defaultValue(-1L);

    private static final Sound ADD_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 1f);
    private static final Sound REMOVE_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 0.1f);
    private static final Sound PLAYER_HURT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1, 1f);
    private static final Sound PLAYER_DEATH_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 1, 1f);

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/parkour", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerStartSpectatorEvent.class, this::initSpectatorPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer)
            .addListener(MapPlayerStartFinishedEvent.class, this::initFinishedPlayer)

            .addListener(MapPlayerCheckpointChangeEvent.class, this::handleCheckpointChange)
            .addListener(MapPlayerStatusChangeEvent.class, this::handleStatusChange)
            .addListener(MapPlayerResetEvent.class, this::handlePlayerReset)
            .addListener(PlayerMoveEvent.class, this::handleInitTimerFromMove)
            .addListener(InstanceTickEvent.class, this::handleTick);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        world.eventNode().addChild(eventNode);

        // Register all the functional items 'silently' so they can only be given by code, not commands or anything.
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
        itemRegistry.registerSilent(ToggleFlightItem.INSTANCE_ON);
        itemRegistry.registerSilent(ToggleFlightItem.INSTANCE_OFF);
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
        if (!event.getMapWorld().isPlaying(player)) return;

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if (event.getMapWorld() instanceof TestingMapWorld) {
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
        inventory.setItemStack(7, itemRegistry.getItemStack(ToggleFlightItem.ID_OFF, null));
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
        if (!event.getMapWorld().isPlaying(player)) return;

        player.updateViewableRule((p) -> true);
        player.removeTag(COUNTDOWN_END);
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
        state.setTimeLimit(-1); // Time always reset on checkpoint
        player.removeTag(COUNTDOWN_END);
        updateStateFromPlayer(player, state);
        updateBaseEffectState(player, data, state);
        if (data.lives() > 0) {
            state.setMaxLives(data.lives());
            state.setLives(data.lives());
        }

        // Cache the last state so that we can reset back here.
        state.setLastState(new SaveState.PlayState(
                Optional.empty(),
                // Set the history to only contain the single checkpoint id so that you can go back to a previous
                // checkpoint though of course this can be prevented by using progress indices
                List.of(event.checkpointId()),
                state.progressIndex(),
                state.timeLimit(),
                state.resetHeight(),
                state.potionEffects().copy(),
                Optional.of(checkpointPos),
                state.maxLives(),
                state.lives()
        ));

        // Update the player based on the new state
        updatePlayerFromState(player, state);
        player.sendMessage(MapMessages.CHECKPOINT_REACHED);
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
        updateStateFromPlayer(player, state);
        updateBaseEffectState(player, data, state);
        if (data.extraTime() > 0 && state.timeLimit().isPresent()) {
            state.setTimeLimit(state.timeLimit().get() + data.extraTime());
        }
        state.addStatus(event.statusId());

        // Update the player based on the new state
        updatePlayerFromState(player, state);
    }

    public void handlePlayerReset(@NotNull MapPlayerResetEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return;

        if (event.toCheckpoint()) {
            softReset(player, saveState);
        } else {
            hardReset(player, saveState);
        }
    }

    private void handleInitTimerFromMove(@NotNull PlayerMoveEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || saveState.getPlayStartTime() != 0) return;

        var oldPosition = player.getPosition();
        var newPosition = event.getNewPosition();
        if (Vec.fromPoint(oldPosition).equals(Vec.fromPoint(newPosition)))
            return; // Player did not actually move, just turn their head

        // Start the timer.
        saveState.setPlayStartTime(System.currentTimeMillis());
    }

    public void handleTick(@NotNull InstanceTickEvent event) {
        var instance = event.getInstance();
        var world = MapWorld.unsafeFromInstance(instance);
        if (world == null) return; // Sanity

        long now = System.currentTimeMillis();

        var players = instance.getEntityTracker().entities(EntityTracker.Target.PLAYERS);
        for (var player : players) {
            if (!world.isPlaying(player)) continue;
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) continue;

            var resetHeight = saveState.playState().resetHeight().orElse(-64);
            if (player.getPosition().y() < resetHeight) {
                softReset(player, saveState);
                continue;
            }

            var countdownEnd = player.getTag(COUNTDOWN_END);
            if (countdownEnd != -1 && countdownEnd < now) {
                player.sendMessage("you ran out of time, todo add a sound effect or something");
                softReset(player, saveState);
                continue;
            }
        }

        // If a player is spectating return them to their checkpoint if they fall below the reset height
        if (MapWorld.unsafeFromInstance(instance) instanceof PlayingMapWorld pmw) {
            for (var spectator : pmw.spectators()) {
                if (spectator.getPosition().y() < pmw.instance().getDimensionType().getMinY()) {
                    var checkpoint = spectator.getTag(SPECTATOR_CHECKPOINT);
                    spectator.teleport(checkpoint == null ? pmw.spawnPoint(spectator) : checkpoint);
                }
            }
        }

    }

    /**
     * Resets the player to the start of the map.
     *
     * @param player    the player to reset
     * @param saveState the save state of the player
     */
    public void hardReset(@NotNull Player player, @NotNull SaveState saveState) {
        if (saveState.isCompleted()) return;
        var world = MapWorld.forPlayerOptional(player);
        if (!(world instanceof AbstractMapWorld abstractWorld)) return;

        // Remove the playing tag so that they can't trigger a checkpoint/status/completion
        abstractWorld.removePlayerImmediate(player);

        saveState.setCompleted(false);
        saveState.setPlaytime(0);
        saveState.setPlayStartTime(0);
        var newPlayState = new SaveState.PlayState();
        saveState.setPlayState(newPlayState);

        player.removeTag(SPECTATOR_CHECKPOINT);
        player.removeTag(COUNTDOWN_END);

        player.teleport(world.map().settings().getSpawnPoint()).thenRun(() -> {
            updatePlayerFromState(player, newPlayState);
            abstractWorld.addPlayerImmediate(player);

            EventDispatcher.call(new MapPlayerInitEvent(world, player, true));
        });
    }

    /**
     * Resets the player to their last checkpoint, or to the start of the map if they don't have a checkpoint.
     *
     * @param player the player to reset
     */
    public void softReset(@NotNull Player player, @NotNull SaveState saveState) {
        if (saveState.isCompleted()) return;
        var world = MapWorld.forPlayerOptional(player);
        if (!(world instanceof AbstractMapWorld abstractWorld)) return;

        // If they have a spectator checkpoint return to that always.
        var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
        if (checkpoint != null) {
            player.teleport(checkpoint);
            return;
        }

        var playState = saveState.playState();
        // If they don't have a checkpoint or are on their last life, do a hard reset.
        var isOutOfLives = playState.lives().orElse(0) == 1;
        if (playState.lastState().isEmpty() || isOutOfLives) {
            if (isOutOfLives) {
                player.playSound(PLAYER_DEATH_SOUND);
            }
            hardReset(player, saveState);
            return;
        }

        // Remove the playing tag so that they can't trigger a checkpoint/status/completion
        abstractWorld.removePlayerImmediate(player);

        // "pop" the last state to the current
        playState = playState.lastState().get();
        if (playState.lives().isPresent()) {
            // This is definitely valid, we checked above to see if this was the last life.
            playState.setLives(playState.lives().get() - 1);
            player.playSound(PLAYER_HURT_SOUND);
        }
        saveState.setPlayState(playState);
        // Create a copy so that we can reset to the checkpoint again
        playState.setLastState(playState.copy());

        player.removeTag(COUNTDOWN_END); // Remove so it is reapplied by updatePlayerFromState
        // Apply the current state to the player and teleport them
        updatePlayerFromState(player, playState);
        player.teleport(playState.pos().orElseThrow()).thenRun(() -> {
            abstractWorld.addPlayerImmediate(player);

            EventDispatcher.call(new MapPlayerInitEvent(world, player, false));
        });
    }

    private void updateBaseEffectState(@NotNull Player player, @NotNull BaseEffectData data, @NotNull SaveState.PlayState state) {
        if (data.progressIndex() != -1) {
            state.setProgressIndex(data.progressIndex());
        }
        if (data.timeLimit() > 0) {
            // Only update the time limit if it is assigned in this effect.
            // In a checkpoint it will have been reset prior to calling this function.
            state.setTimeLimit(data.timeLimit());
        }
        if (data.resetHeight() != BaseEffectData.NO_RESET_HEIGHT) {
            state.setResetHeight(data.resetHeight());
        }
        if (data.clearPotionEffects()) {
            // Play 'remove' effect if we had effects and they were removed
            if (!state.potionEffects().isEmpty() && data.potionEffects().isEmpty()) {
                player.playSound(REMOVE_EFFECTS_SOUND);
            }
            state.potionEffects().clear();
        }
        if (!data.potionEffects().isEmpty()) {
            player.playSound(ADD_EFFECTS_SOUND);
            for (var newEffect : data.potionEffects().entries()) {
                var existingEffect = state.potionEffects().getOrCreate(newEffect.type());
                existingEffect.setLevel(newEffect.level());
                existingEffect.setDuration(newEffect.duration());
            }
        }
        if (data.teleport().isPresent()) {
            player.teleport(data.teleport().get()); //todo teleport is not immediate, need to handle that.
        }
    }

    private void updateStateFromPlayer(@NotNull Player player, @NotNull SaveState.PlayState state) {
        long now = System.currentTimeMillis();

        var countdownEnd = player.getTag(COUNTDOWN_END);
        if (countdownEnd == -1) return;
        state.setTimeLimit(countdownEnd - now);

        for (var timedPotion : player.getActiveEffects()) {
            var potion = timedPotion.getPotion();
            var effectType = PotionInfo.getByVanillaEffect(potion.effect());
            if (effectType == null) continue;

            var entry = state.potionEffects().get(effectType);
            if (entry == null) continue;

            // Potion is valid, update the time remaining
            entry.setDuration(Math.max(0, (int) (potion.duration() - (now - timedPotion.getStartingTime()))));
        }
    }

    private void updatePlayerFromState(@NotNull Player player, @NotNull SaveState.PlayState state) {
        // Set the player health to the number of lives they have (1 heart = 1 life)
        if (state.maxLives().isPresent() && state.lives().isPresent()) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2 * state.maxLives().get());
            player.setHealth(2 * state.lives().get());
        } else {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Attribute.MAX_HEALTH.defaultValue());
            player.setHealth(Attribute.MAX_HEALTH.defaultValue());
        }

        // Update the countdown timer (time may have been added
        if (state.timeLimit().isPresent()) {
            player.setTag(COUNTDOWN_END, System.currentTimeMillis() + state.timeLimit().get());
        }

        // Update the potions on the player
        player.clearEffects();
        for (var entry : state.potionEffects().entries()) {
            player.addEffect(new Potion(
                    entry.type().vanillaEffect(),
                    (byte) (entry.level() - 1),
                    entry.duration() <= 0 ? Potion.INFINITE_DURATION : entry.duration() / MinecraftServer.TICK_MS, // Convert from milliseconds to ticks
                    Potion.ICON_FLAG
            ));
        }
    }

    private void updateViewership(@NotNull MapWorld world) {
        for (Player p : world.players()) {
            p.updateViewableRule();
        }
    }

}

package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartFinishedEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerStartSpectatorEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.feature.play.item.*;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.map.util.PlayerVisibilityExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.MinecraftServer;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.potion.Potion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {
    private static final int RESET_HEIGHT_OFFSET = 5;
    private static final Tag<Integer> DEFAULT_RESET_HEIGHT = Tag.Integer("mapmaker:play/reset_height").defaultValue(-64 - RESET_HEIGHT_OFFSET);

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
            .addListener(PlayerTickEvent.class, this::handlePlayerTick);

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

        computeDefaultResetHeight(world.instance());

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        MapCompletionAnimation.cancel(player); // In case the player resets themselves

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
            if (MapRatingFeatureProvider.isMapRatable(event.mapWorld())) {
                inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));
            }

            if (!event.getMap().getSetting(MapSettings.NO_SPECTATOR)) {
                inventory.setItemStack(4, itemRegistry.getItemStack(EnterSpectatorModeItem.ID, null));
            }

            inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));
        }

        var visibilityPredicate = new PlayerVisibilityPredicate(player);
        player.updateViewerRule(visibilityPredicate);
        if (player instanceof PlayerVisibilityExtension ve)
            ve.setVisibilityFunc(visibilityPredicate);

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) updatePlayerFromState(player, saveState.playState());
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

        // Only visibility extension, no viewer rule (spectators can see anyone)
        player.updateViewerRule(null);
        if (player instanceof PlayerVisibilityExtension ve)
            ve.setVisibilityFunc(new PlayerVisibilityPredicate(player));
    }

    public void initFinishedPlayer(@NotNull MapPlayerStartFinishedEvent event) {
        var player = event.getPlayer();

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
        inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));

        inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
        inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));

        // Only visibility extension, no viewer rule (spectators can see anyone)
        player.updateViewerRule(null);
        if (player instanceof PlayerVisibilityExtension ve)
            ve.setVisibilityFunc(new PlayerVisibilityPredicate(player));
    }

    public void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            var countdownEnd = player.getTag(COUNTDOWN_END);
            if (countdownEnd != -1) {
                saveState.playState().setTimeLimit(countdownEnd - System.currentTimeMillis());
            }
        }

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

    public void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        if (world.isPlaying(player)) {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) return;

            var resetHeight = saveState.playState().resetHeight().orElse(world.instance().getTag(DEFAULT_RESET_HEIGHT));
            if (player.getPosition().y() < resetHeight) {
                softReset(player, saveState);
                return;
            }

            var countdownEnd = player.getTag(COUNTDOWN_END);
            if (countdownEnd != -1 && countdownEnd < System.currentTimeMillis()) {
                player.sendMessage("you ran out of time, todo add a sound effect or something");
                softReset(player, saveState);
                return;
            }
        } else if (world.isSpectating(player)) {
            if (player.getPosition().y() < world.instance().getDimensionType().getMinY()) {
                var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
                player.teleport(checkpoint == null ? world.spawnPoint(player) : checkpoint);
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
            // If the checkpoint is below the reset height, teleport to the spawn instead to prevent getting stuck.
            // If they set the spawn below the world then its a joke map anyway and i don't care.
            var resetHeight = saveState.playState().resetHeight().orElse(world.instance().getTag(DEFAULT_RESET_HEIGHT));
            if (checkpoint.y() < resetHeight) {
                player.teleport(world.spawnPoint(player));
            } else {
                player.teleport(checkpoint);
            }
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
            var potion = timedPotion.potion();
            var effectType = PotionInfo.getByVanillaEffect(potion.effect());
            if (effectType == null) continue;

            var entry = state.potionEffects().get(effectType);
            if (entry == null) continue;

            // Potion is valid, update the time remaining
            //todo convert all to ticks
            int remainingWallTime = (int) ((potion.duration() - (player.getInstance().getWorldAge() - timedPotion.startingTicks())) * 50);
            entry.setDuration(Math.max(0, remainingWallTime));
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
        } else {
            player.removeTag(COUNTDOWN_END);
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
            p.updateViewerRule(); // Only players have special viewable rules
            if (p instanceof PlayerVisibilityExtension ve)
                ve.updateVisibility();
        }
        for (Player p : world.spectators()) {
            if (p instanceof PlayerVisibilityExtension ve)
                ve.updateVisibility();
        }
    }

    private void computeDefaultResetHeight(@NotNull Instance instance) {
        int worldMinHeight = instance.getDimensionType().getMinY();
        int minBlockY = instance.getDimensionType().getMaxY();

        int worldRadius = (int) (instance.getWorldBorder().getDiameter() / 2) + 16;
        worldRadius = Math.min(worldRadius, 4096); // Prevent infinite computation
        for (int x = -worldRadius; x < worldRadius; x += 16) {
            for (int z = -worldRadius; z < worldRadius; z += 16) {
                var rawChunk = instance.getChunkAt(x, z);
                if (!(rawChunk instanceof ChunkExt chunk)) continue;

                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int lowestBlockY = chunk.getHeight(Heightmaps.WORLD_BOTTOM, localX, localZ);
                        if (lowestBlockY >= worldMinHeight) minBlockY = Math.min(minBlockY, lowestBlockY);
                    }
                }
            }
        }

        // Sanity check in case there are literally no blocks in the world.
        if (minBlockY == instance.getDimensionType().getMaxY()) minBlockY = worldMinHeight;
        instance.setTag(DEFAULT_RESET_HEIGHT, minBlockY - RESET_HEIGHT_OFFSET);
    }

    private static class PlayerVisibilityPredicate implements Predicate<Entity>, Function<Player, PlayerVisibilityExtension.Visibility> {
        // This implements a viewER (not viewABLE) predicate for each player. This is used to determine if the player
        // (the one in the field `player`) can see each other entity. If `#test` returns true, they will be visible.
        // Otherwise, they will not.
        // Additionally, this implements a visibility function to decide when to make another player a ghost.

        private static final double PLAYER_HIDE_DISTANCE = 3.5;
        private static final double SPECTATOR_HIDE_DISTANCE = PLAYER_HIDE_DISTANCE * 2;

        private final Player player;
        private final PlayerDataV2 playerData;
        private final MapWorld world;

        private PlayerVisibilityPredicate(@NotNull Player player) {
            this.player = player;
            this.playerData = PlayerDataV2.fromPlayer(player);
            this.world = MapWorld.forPlayerOptional(player);
        }

        @Override
        public boolean test(@NotNull Entity otherEntity) {
            if (!(otherEntity instanceof Player other)) return true; // Always show non-players
            if (world.isPlaying(other) || (world instanceof TestingMapWorld testWorld && testWorld.buildWorld().isPlaying(other))) {
                var rule = playerData.getSetting(PlayerSettings.NEARBY_PLAYER_VISIBILITY);
                // If the ghost rule is used then we never hide the player (but they will become invisible)
                if (rule == VisibilityRule.GHOST) return true;
                // Otherwise, hide the player if they are too close
                return player.getDistanceSquared(other) > PLAYER_HIDE_DISTANCE * PLAYER_HIDE_DISTANCE;
            } else if (world.isSpectating(other)) {
                // Always hide spectators if they are too close. Note that this does not execute for spectators
                // so this will not stop them from seeing each other.
                return player.getDistanceSquared(other) > SPECTATOR_HIDE_DISTANCE * SPECTATOR_HIDE_DISTANCE;
            } else return true;
        }

        // Called for every other player, returns how we should be visible to them
        @Override
        public @NotNull PlayerVisibilityExtension.Visibility apply(Player other) {
            if (world.isPlaying(other) || (world instanceof TestingMapWorld testWorld && testWorld.buildWorld().isPlaying(other))) {
                if (player.getDistanceSquared(other) > PLAYER_HIDE_DISTANCE * PLAYER_HIDE_DISTANCE)
                    return PlayerVisibilityExtension.Visibility.VISIBLE;
                return PlayerVisibilityExtension.Visibility.INVISIBLE;
            } else if (world.isSpectating(player)) {
                return PlayerVisibilityExtension.Visibility.SPECTATOR;
            } else return PlayerVisibilityExtension.Visibility.VISIBLE;
        }
    }

}

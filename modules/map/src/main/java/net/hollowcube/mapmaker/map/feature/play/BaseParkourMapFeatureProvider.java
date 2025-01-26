package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import io.prometheus.client.Counter;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.event.*;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointPreChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItem;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.feature.play.item.*;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.util.CustomizableHotbarManager;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.map.util.PlayerVisibilityExtension;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerStopFlyingWithElytraEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {
    private static final int THIRTY_MINUTES = 30 * 60 * 1000;
    private static final Counter RESETS_AFTER_30_MINUTES = Counter.build()
            .name("reset_after_30_minutes_count")
            .help("Number of times a player has reset after 30 minutes of play")
            .register();

    private static final int RESET_HEIGHT_OFFSET = 5;
    private static final Tag<Integer> DEFAULT_RESET_HEIGHT = Tag.Integer("mapmaker:play/reset_height").defaultValue(-64 - RESET_HEIGHT_OFFSET);

    private static final TagCooldown PROGRESS_INDEX_WARNING = new TagCooldown("mapmaker:play/progress_index_warning", 5000);

    // This tag is present when the player has an active countdown and holds the time at which
    // the countdown will end, in ms since epoch.
    public static final Tag<Long> COUNTDOWN_END = Tag.Long("mapmaker:play/countdown_end").defaultValue(-1L);

    // Holds the CheckpointEffectData applied to the player on first spawn.
    public static final Tag<CheckpointEffectData> SPAWN_CHECKPOINT_EFFECTS = DFU.Tag(CheckpointEffectData.CODEC, "spawn_checkpoint_effects");

    private static final Sound ADD_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 1f);
    private static final Sound REMOVE_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 0.1f);
    private static final Sound PLAYER_HURT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1, 1f);
    private static final Sound PLAYER_DEATH_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 1, 1f);

    private static final AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 500, AttributeOperation.ADD_VALUE);

    private static final Equippable EMPTY_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            null, null, null, false,
            false, false);
    private static final Equippable ELYTRA_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            "minecraft:elytra", null, null,
            false, false, false);

    private static final CustomizableHotbarManager TESTING_HOTBAR = CustomizableHotbarManager.builder("hotbar/parkour/test")
            .defaultItem(0, MapDetailsItem.ID)
            .defaultItem(1, ReturnToCheckpointItem.ID)
            .defaultItem(2, SetSpectatorCheckpointItem.ID_TESTING)

            .defaultItem(7, ResetSaveStateItem.ID)
            .defaultItem(8, ExitTestModeItem.ID)
            .build();

    private static final CustomizableHotbarManager PLAYING_HOTBAR = CustomizableHotbarManager.builder("hotbar/parkour")
            .defaultItem(0, MapDetailsItem.ID)
            .defaultItem(1, ReturnToCheckpointItem.ID)
            .defaultItem(2, RateMapItem.ID, (player, world) -> MapRatingFeatureProvider.isMapRatable(world))

            .defaultItem(4, EnterSpectatorModeItem.ID, (player, world) -> !world.map().getSetting(MapSettings.NO_SPECTATOR))

            .defaultItem(7, ResetSaveStateItem.ID)
            .defaultItem(8, ReturnToHubItem.ID)
            .build();

    private static final CustomizableHotbarManager SPEC_HOTBAR = CustomizableHotbarManager.builder("hotbar/parkour/spec")
            .defaultItem(0, MapDetailsItem.ID)
            .defaultItem(1, ReturnToSpectatorCheckpointItem.ID)
            .defaultItem(2, SetSpectatorCheckpointItem.ID_SPECTATOR)

            .defaultItem(4, ExitSpectatorModeItem.ID)

            .defaultItem(7, ToggleFlightItem.ID_OFF)
            .defaultItem(8, ReturnToHubItem.ID)
            .build();

    private static final CustomizableHotbarManager FINISH_HOTBAR = CustomizableHotbarManager.builder("hotbar/parkour/finish")
            .defaultItem(0, MapDetailsItem.ID)
            .defaultItem(2, RateMapItem.ID, (player, world) -> MapRatingFeatureProvider.isMapRatable(world))

            .defaultItem(7, ResetSaveStateItem.ID)
            .defaultItem(8, ReturnToHubItem.ID)
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/parkour", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerStartSpectatorEvent.class, this::initSpectatorPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer)
            .addListener(MapPlayerStartFinishedEvent.class, this::initFinishedPlayer)

            .addListener(MapPlayerCheckpointPreChangeEvent.class, this::handleCheckpointChange)
            .addListener(MapPlayerStatusChangeEvent.class, this::handleStatusChange)
            .addListener(MapPlayerResetEvent.class, this::handlePlayerReset)
            .addListener(PlayerMoveEvent.class, this::handleInitTimerFromMove)
            .addListener(PlayerTickEvent.class, this::handlePlayerTick)

            .addListener(InventoryPreClickEvent.class, event -> {
                if (MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(event.getPlayer()))
                    return;
                event.setCancelled(true);
            });

//            .addListener(PlayerBlockInteractEvent.class, event -> {
//                var block = event.getBlock();
//                if (BlockTags.TRAPDOORS.contains(block.namespace())) {
//                    var ghostBlocks = GhostBlockHolder.forPlayer(event.getPlayer());
//                    block = ghostBlocks.getBlock(event.getBlockPosition());
//                    var newOpen = String.valueOf("false".equals(block.getProperty("open")));
    /// /                    event.getInstance().setBlock(event.getBlockPosition(), block.withProperty("open", newOpen));
//                    ghostBlocks.setBlock(event.getBlockPosition(), block.withProperty("open", newOpen));
//
//                    event.setCancelled(true);
//                    event.setBlockingItemUse(true);
//                    return;
//                }
//
//                var itemStack = event.getPlayer().getItemInHand(event.getHand());
//                if (!itemStack.material().isBlock()) return;
//
//                var placedBlock = itemStack.material().block();
//                var placePosition = event.getBlockPosition().relative(event.getBlockFace());
//
//                var ghostBlocks = GhostBlockHolder.forPlayer(event.getPlayer());
//                ghostBlocks.setBlock(placePosition, placedBlock);
//
//                event.setCancelled(true); // Prevent processing block entity
//                event.setBlockingItemUse(true); // Send ack (we have already sent block so the client will interpret as successful place)
//            });

    private static final Tag<Boolean> RESET_TAG = Tag.Boolean("mapmaker:play/reset").defaultValue(false);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;
        if (world.map().settings().getVariant() != MapVariant.PARKOUR)
            return false;

        if (world instanceof TestingMapWorld) {
            TESTING_HOTBAR.registerEvents(eventNode);
        } else {
            PLAYING_HOTBAR.registerEvents(eventNode);
            SPEC_HOTBAR.registerEvents(eventNode);
            FINISH_HOTBAR.registerEvents(eventNode);
        }
        world.eventNode().addChild(eventNode);

        // Register all the functional items 'silently' so they can only be given by code, not commands or anything.
        var itemRegistry = world.itemRegistry();
        itemRegistry.registerSilent(CustomizableHotbarManager.RESET_TO_DEFAULT_ITEM);
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
        var world = event.getMapWorld();
        if (!world.isPlaying(player)) return;

        MapCompletionAnimation.cancel(player); // In case the player resets themselves

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if (MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) {
            if (event.getMapWorld() instanceof TestingMapWorld) {
                TESTING_HOTBAR.apply(player, world);
            } else {
                PLAYING_HOTBAR.apply(player, world);
            }
        } else {
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
        }

        player.scheduleNextTick(ignored -> {
            // This must happen on the tick thread, it requires a lock on nearby players
            var visibilityPredicate = new PlayerVisibilityPredicate(player);
            player.updateViewerRule(visibilityPredicate);
            if (player instanceof PlayerVisibilityExtension ve)
                ve.setVisibilityFunc(visibilityPredicate);
        });

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            var playState = saveState.state(PlayState.class);
            var isStarting = saveState.getPlayStartTime() == 0 && saveState.getPlaytime() == 0;

            // If this is a fresh save state, attempt to add the base effect state
            if (world.hasTag(SPAWN_CHECKPOINT_EFFECTS) && isStarting) {
                updateCheckpointEffectState(world, player, world.getTag(SPAWN_CHECKPOINT_EFFECTS), playState);
            }

            updatePlayerFromState(world, player, playState, isStarting);

            // If this is OS, reset the player as they are added
            if (world.map().settings().isOnlySprint() && !player.getTag(RESET_TAG)) {
                player.setTag(RESET_TAG, true);
                player.scheduleNextTick(ignored -> player.removeTag(RESET_TAG));
//                player.sendMessage(Component.translatable("map.spectator_mode.only_sprint"));
                softReset(player, saveState);
            }
        }

        //todo this should not be applied if fall damage is enabled
        player.getAttribute(Attribute.SAFE_FALL_DISTANCE).addModifier(NO_FALL_DAMAGE_MODIFIER);
    }

    public void initSpectatorPlayer(@NotNull MapPlayerStartSpectatorEvent event) {
        var player = event.getPlayer();

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if (MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) {
            SPEC_HOTBAR.apply(player, event.getMapWorld());
        } else {
            inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
            inventory.setItemStack(1, itemRegistry.getItemStack(ReturnToSpectatorCheckpointItem.ID, null));
            inventory.setItemStack(2, itemRegistry.getItemStack(SetSpectatorCheckpointItem.ID_SPECTATOR, null));
            inventory.setItemStack(4, itemRegistry.getItemStack(ExitSpectatorModeItem.ID, null));
            inventory.setItemStack(7, itemRegistry.getItemStack(ToggleFlightItem.ID_OFF, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));
        }

        player.scheduleNextTick(ignored -> {
            // Only visibility extension, no viewer rule (spectators can see anyone)
            // Must happen on tick thread, needs lock on nearby players to update viewer rule.
            player.updateViewerRule(null);
            if (player instanceof PlayerVisibilityExtension ve)
                ve.setVisibilityFunc(new PlayerVisibilityPredicate(player));
        });
    }

    public void initFinishedPlayer(@NotNull MapPlayerStartFinishedEvent event) {
        var player = event.getPlayer();

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if (MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) {
            FINISH_HOTBAR.apply(player, event.getMapWorld());
        } else {
            inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
            if (MapRatingFeatureProvider.isMapRatable(event.mapWorld())) {
                inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));
            }

            inventory.setItemStack(7, itemRegistry.getItemStack(ResetSaveStateItem.ID, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));
        }

        player.scheduleNextTick(ignored -> {
            // Only visibility extension, no viewer rule (spectators can see anyone)
            player.updateViewerRule(null);
            if (player instanceof PlayerVisibilityExtension ve)
                ve.setVisibilityFunc(new PlayerVisibilityPredicate(player));
        });
    }

    public void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        CustomizableHotbarManager.unregister(player);

        if (!event.getMapWorld().isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            var playState = saveState.state(PlayState.class);
            updateStateFromPlayer(player, playState);
        }

        player.removeTag(COUNTDOWN_END);

        player.getAttribute(Attribute.SAFE_FALL_DISTANCE).removeModifier(NO_FALL_DAMAGE_MODIFIER);
    }

    public void handleCheckpointChange(@NotNull MapPlayerCheckpointPreChangeEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        var state = SaveState.fromPlayer(player).state(PlayState.class);
        var data = event.effectData();

        // Ensure the event should trigger a checkpoint change for the current players state
        if (checkProgressIndex(player, world, state, data)) return;
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
        updateCheckpointEffectState(world, player, data, state);

        List<String> newHistory;
        if (world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION)) {
            // With additive progress index you can never touch a previous checkpoint
            state.history().add(event.checkpointId());
            newHistory = List.copyOf(state.history());
        } else {
            // Set the history to only contain the single checkpoint id so that you can go back to a previous
            // checkpoint though of course this can be prevented by using progress indices
            newHistory = List.of(event.checkpointId());
        }

        // Cache the last state so that we can reset back here.
        state.setLastState(new PlayState(
                Optional.empty(),
                newHistory,
                state.progressIndex(),
                state.timeLimit(),
                state.resetHeight(),
                state.potionEffects().copy(),
                Optional.of(checkpointPos),
                state.maxLives(),
                state.lives(),
                Map.copyOf(state.ghostBlocks()),
                state.items(),
                state.settings().copy()
        ));

        // Update the player based on the new state
        updatePlayerFromState(event.getMapWorld(), player, state);

        event.getMapWorld().callEvent(new MapPlayerCheckpointChangeEvent(player, event.getMapWorld(), event.checkpointId(), data));
        player.sendMessage(MapMessages.CHECKPOINT_REACHED);
    }

    public void handleStatusChange(@NotNull MapPlayerStatusChangeEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        var state = SaveState.fromPlayer(player).state(PlayState.class);
        var data = event.effectData();

        // Ensure the event should trigger a status change for the current players state
        if (!data.repeatable() && state.hasStatus(event.statusId()))
            return; // Player already has the status plate in this checkpoint.
        if (checkProgressIndex(player, world, state, data)) return;

        // Apply the status changes
        updateStateFromPlayer(player, state);
        updateBaseEffectState(world, player, data, state);
        if (data.extraTime() > 0 && state.timeLimit().isPresent()) {
            state.setTimeLimit(state.timeLimit().get() + data.extraTime());
        }
        state.addStatus(event.statusId());
        state.settings().update(data.settings());

        // Update the player based on the new state
        updatePlayerFromState(world, player, state);
    }

    private boolean checkProgressIndex(@NotNull Player player, @NotNull MapWorld world, @NotNull PlayState state, @NotNull BaseEffectData data) {
        if (data.progressIndex() > 0) {
            int currentIndex = state.progressIndex().orElse(0);
            boolean condition = world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION)
                    // With additive index you can get anything <= current + 1
                    ? (data.progressIndex() > currentIndex + 1)
                    // Without additive progress index you must be at the prior index or the current one
                    : (data.progressIndex() != currentIndex && data.progressIndex() != currentIndex + 1);
            if (condition) {
                if (PROGRESS_INDEX_WARNING.test(player)) {
                    player.sendMessage(Component.translatable("checkpoint.progress_index.not_acceptable",
                            Component.text(currentIndex), Component.text(data.progressIndex())));
                }
                return true;
            }
        }
        return false;
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

        var effects = world.getTag(SPAWN_CHECKPOINT_EFFECTS);
        if (effects != null && effects.timeLimit() > 0) {
            player.setTag(COUNTDOWN_END, System.currentTimeMillis() + effects.timeLimit());
        }
    }

    public void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        if (world.isPlaying(player)) {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) return;

            var playState = saveState.state(PlayState.class);
            var resetHeight = playState.resetHeight().orElse(world.instance().getTag(DEFAULT_RESET_HEIGHT));
            if (player.getPosition().y() < resetHeight) {
                softReset(player, saveState);
                return;
            }

            var countdownEnd = player.getTag(COUNTDOWN_END);
            if (countdownEnd != -1 && countdownEnd < System.currentTimeMillis()) {
                player.sendMessage(Component.translatable("playing.timer.run_out"));
                softReset(player, saveState);
                return;
            }
        } else if (world.isSpectating(player)) {
            if (player.getPosition().y() < world.instance().getCachedDimensionType().minY()) {
                var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
                resetTeleport(player, checkpoint == null ? world.spawnPoint(player) : checkpoint);
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

        // iTMG thinks that this happens a lot and we should add a feature to let people
        if (saveState.getRealPlaytime() > THIRTY_MINUTES) {
            RESETS_AFTER_30_MINUTES.inc();
        }

        saveState.setCompleted(false);
        saveState.setPlaytime(0);
        saveState.setPlayStartTime(0);
        var newPlayState = new PlayState();
        saveState.setState(newPlayState);

        player.removeTag(SPECTATOR_CHECKPOINT);
        player.removeTag(COUNTDOWN_END);

        resetTeleport(player, world.map().settings().getSpawnPoint()).thenRun(() -> {
            updatePlayerFromState(world, player, newPlayState, true);
            abstractWorld.addPlayerImmediate(player);

            EventDispatcher.call(new MapPlayerInitEvent(world, player, true, false));
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
            var playState = saveState.state(PlayState.class);
            var resetHeight = playState.resetHeight().orElse(world.instance().getTag(DEFAULT_RESET_HEIGHT));
            if (checkpoint.y() < resetHeight) {
                resetTeleport(player, world.spawnPoint(player)).thenRun(() -> {
                    EventDispatcher.call(new MapPlayerInitEvent(world, player, false, false));
                });
            } else {
                resetTeleport(player, checkpoint).thenRun(() -> {
                    EventDispatcher.call(new MapPlayerInitEvent(world, player, false, false));
                });
            }
            return;
        }

        var playState = saveState.state(PlayState.class);
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
        saveState.setState(playState);
        // Create a copy so that we can reset to the checkpoint again
        playState.setLastState(playState.copy());

        player.removeTag(COUNTDOWN_END); // Remove so it is reapplied by updatePlayerFromState
        // Apply the current state to the player and teleport them
        updatePlayerFromState(world, player, playState);
        resetTeleport(player, playState.pos().orElseThrow()).thenRun(() -> {
            abstractWorld.addPlayerImmediate(player);

            EventDispatcher.call(new MapPlayerInitEvent(world, player, false, false));
        });
    }

    private void updateCheckpointEffectState(@NotNull MapWorld world, @NotNull Player player, @NotNull CheckpointEffectData data, @NotNull PlayState state) {
        updateBaseEffectState(world, player, data, state);
        if (data.lives() > 0) {
            state.setMaxLives(data.lives());
            state.setLives(data.lives());
        } else {
            state.setMaxLives(-1);
            state.setLives(-1);
        }
        state.settings().update(data.settings());
    }

    private void updateBaseEffectState(@NotNull MapWorld world, @NotNull Player player, @NotNull BaseEffectData data, @NotNull PlayState state) {
        if (data.progressIndex() != -1) {
            boolean useProgressAddition = world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION);
            state.setProgressIndex(useProgressAddition ? (state.progressIndex().orElse(0) + data.progressIndex()) : data.progressIndex());
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
            resetTeleport(player, data.teleport().get()).thenRun(() -> {
                player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_TELEPORT, Sound.Source.PLAYER, 0.5f, 1f), player.getPosition());
            });
        }
        var newItem1 = state.items().item1();
        if (data.items().item1() != null) newItem1 = data.items().item1();
        var newItem2 = state.items().item2();
        if (data.items().item2() != null) newItem2 = data.items().item2();
        var newItem3 = state.items().item3();
        if (data.items().item3() != null) newItem3 = data.items().item3();
        var newElytra = state.items().elytra();
        if (data.items().elytra() != null) newElytra = data.items().elytra();

        state.setItems(new HotbarItems(newItem1, newItem2, newItem3, newElytra));
    }

    private void updateStateFromPlayer(@NotNull Player player, @NotNull PlayState state) {
        long now = System.currentTimeMillis();

        var countdownEnd = player.getTag(COUNTDOWN_END);
        if (countdownEnd != -1) {
            state.setTimeLimit(countdownEnd - now);
        }

        // Update remaining time for the remaining effects (and remove if expired)
        var iter = state.potionEffects().entries().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.duration() <= 0) continue; // No need to update if infinite

            final TimedPotion activeEffect = player.getEffect(entry.type().vanillaEffect());
            if (activeEffect == null) {
                iter.remove(); // Expired effect
                continue;
            }

            // Otherwise, update the duration
            //todo convert all to ticks
            int remainingWallTime = (int) ((activeEffect.potion().duration() - (player.getAliveTicks() - activeEffect.startingTicks())) * 50);
            entry.setDuration(Math.max(0, remainingWallTime));
        }

        var ghostBlocks = GhostBlockHolder.forPlayerOptional(player);
        state.setGhostBlocks(ghostBlocks == null ? Map.of() : ghostBlocks.save());

        var items = state.items();
        state.setItems(new HotbarItems(
                items.item1() == null ? HotbarItem.Remove.INSTANCE : items.item1().fromItemStack(player.getInventory().getItemStack(3)),
                items.item2() == null ? HotbarItem.Remove.INSTANCE : items.item2().fromItemStack(player.getInventory().getItemStack(5)),
                items.item3() == null ? HotbarItem.Remove.INSTANCE : items.item3().fromItemStack(player.getInventory().getItemStack(6)),
                items.elytra()
        ));
    }

    private void updatePlayerFromState(MapWorld world, @NotNull Player player, @NotNull PlayState state) {
        updatePlayerFromState(player, state, false);
    }

    private void updatePlayerFromState(MapWorld world, @NotNull Player player, @NotNull PlayState state, boolean start) {
        // Set the player health to the number of lives they have (1 heart = 1 life)
        if (state.maxLives().isPresent() && state.lives().isPresent()) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2 * state.maxLives().get());
            player.setHealth(2 * state.lives().get());
        } else {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Attribute.MAX_HEALTH.defaultValue());
            player.setHealth((float) Attribute.MAX_HEALTH.defaultValue());
        }

        // Update the countdown timer (time may have been added
        if (state.timeLimit().isPresent() && !start) {
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

        if (!state.ghostBlocks().isEmpty()) {
            var ghostBlocks = GhostBlockHolder.forPlayer(player);
            ghostBlocks.load(state.ghostBlocks());
        }

        // Apply items to current state.
        var item1 = state.items().item1();
        player.getInventory().setItemStack(3, item1 == null || item1 instanceof HotbarItem.Remove
                ? ItemStack.AIR : item1.toItemStack(false));
        var item2 = state.items().item2();
        player.getInventory().setItemStack(5, item2 == null || item2 instanceof HotbarItem.Remove
                ? ItemStack.AIR : item2.toItemStack(false));
        var item3 = state.items().item3();
        player.getInventory().setItemStack(6, item3 == null || item3 instanceof HotbarItem.Remove
                ? ItemStack.AIR : item3.toItemStack(false));
        if (Objects.requireNonNullElse(state.items().elytra(), false)) {
            player.setChestplate(player.getChestplate().with(ItemComponent.GLIDER)
                    .with(ItemComponent.EQUIPPABLE, ELYTRA_EQUIPPABLE));
        } else {
            player.setChestplate(player.getChestplate().without(ItemComponent.GLIDER)
                    .with(ItemComponent.EQUIPPABLE, EMPTY_EQUIPPABLE));
        }

        world.callEvent(new MapPlayerUpdateStateEvent(world, player));
    }

    private void updateViewership(@NotNull MapWorld world) {
        for (Player p : Set.copyOf(world.players())) {
            p.updateViewerRule(); // Only players have special viewable rules
            if (p instanceof PlayerVisibilityExtension ve)
                ve.updateVisibility();
        }
        for (Player p : Set.copyOf(world.spectators())) {
            if (p instanceof PlayerVisibilityExtension ve)
                ve.updateVisibility();
        }
    }

    private void computeDefaultResetHeight(@NotNull Instance instance) {
        int worldMinHeight = instance.getCachedDimensionType().minY();
        int minBlockY = instance.getCachedDimensionType().maxY();

        int worldRadius = (int) (instance.getWorldBorder().diameter() / 2) + 16;
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
        if (minBlockY == instance.getCachedDimensionType().maxY()) minBlockY = worldMinHeight;
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
//                return player.getDistanceSquared(other) > PLAYER_HIDE_DISTANCE * PLAYER_HIDE_DISTANCE;
                return false; // Always hide all others
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

    private static CompletableFuture<Void> resetTeleport(@NotNull Player player, @NotNull Pos position) {
        FireworkRocketItem.removeRocket(player);
        player.getPlayerMeta().setFlyingWithElytra(false);
        player.getInstance().eventNode().call(new PlayerStopFlyingWithElytraEvent(player));
        return player.teleport(position, Vec.ZERO, null, RelativeFlags.NONE);
    }

}

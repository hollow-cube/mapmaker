package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.common.events.PlayerMoveVehicleEvent;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.impl.EditLivesAction;
import net.hollowcube.mapmaker.map.action.impl.EditTimerAction;
import net.hollowcube.mapmaker.map.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.map.action.impl.TeleportAction;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.event.*;
import net.hollowcube.mapmaker.map.event.vnext.*;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.feature.play.item.*;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.hollowcube.mapmaker.map.util.CustomizableHotbarManager;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
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
import net.minestom.server.item.component.Equippable;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class BaseParkourMapFeatureProvider implements FeatureProvider {
    static {
        MinecraftServer.getPacketListenerManager().setPlayListener(
                ClientPlayerBlockPlacementPacket.class,
                BlockPlacementFeatureProvider::handleBlockPlacementPacket);
    }

    private static final int RESET_HEIGHT_OFFSET = 5;
    private static final Tag<Integer> DEFAULT_RESET_HEIGHT = Tag.Integer("mapmaker:play/reset_height").defaultValue(-64 - RESET_HEIGHT_OFFSET);

    private static final TagCooldown PROGRESS_INDEX_WARNING = new TagCooldown("mapmaker:play/progress_index_warning", 5000);

    // This tag is present when the player has an active countdown and holds the time at which
    // the countdown will end, in ms since epoch.
    public static final Tag<Long> COUNTDOWN_END = Tag.Long("mapmaker:play/countdown_end").defaultValue(-1L);

    // Holds the CheckpointEffectData applied to the player on first spawn.
    public static final Tag<CheckpointEffectDataV2> SPAWN_CHECKPOINT_EFFECTS = DFU.Tag(CheckpointEffectDataV2.CODEC, "spawn_checkpoint_effects");

    private static final Sound ADD_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 1f);
    private static final Sound REMOVE_EFFECTS_SOUND = Sound.sound(SoundEvent.BLOCK_BREWING_STAND_BREW, Sound.Source.BLOCK, 1, 0.1f);
    private static final Sound PLAYER_HURT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1, 1f);
    private static final Sound PLAYER_DEATH_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 1, 1f);

    private static final AttributeModifier NO_FALL_DAMAGE_MODIFIER = new AttributeModifier("mapmaker:play.no_fall_damage", 500, AttributeOperation.ADD_VALUE);

    private static final Equippable EMPTY_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            null, null, null, false, false, false, false);
    private static final Equippable ELYTRA_EQUIPPABLE = new Equippable(EquipmentSlot.CHESTPLATE, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
            "minecraft:elytra", null, null, false, false, false, false);

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
            .defaultItem(2, RateMapItem.ID, (_, world) -> MapRatingFeatureProvider.isMapRatable(world))

            .defaultItem(7, ResetSaveStateItem.ID)
            .defaultItem(8, ReturnToHubItem.ID)
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/parkour", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapPlayerStartSpectatorEvent.class, this::initSpectatorPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer)
            .addListener(MapPlayerStartFinishedEvent.class, this::initFinishedPlayer)

            .addListener(MapPlayerCheckpointPreChangeEvent.class, this::handleCheckpointChange)
            .addListener(MapPlayerCheckpointPostChangeEvent.class, this::handleCheckpointPostChange)
            .addListener(MapPlayerStatusChangeEvent.class, this::handleStatusChange)
            .addListener(MapPlayerResetEvent.class, this::handlePlayerReset)
            .addListener(PlayerMoveEvent.class, event -> handleInitTimerFromMove(event.getPlayer(), event.getNewPosition()))
            .addListener(PlayerMoveVehicleEvent.class, event -> handleInitTimerFromMove(event.getPlayer(), event.getNewPosition()))
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

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            var playState = saveState.state(PlayState.class);
            var isStarting = saveState.getPlayStartTime() == 0 && saveState.getPlaytime() == 0;

            // If this is a fresh save state, attempt to add the base effect state
            var spawnCheckpoint = world.getTag(SPAWN_CHECKPOINT_EFFECTS);
            if (spawnCheckpoint != null && isStarting) {
                updateBaseEffectState(world, player, spawnCheckpoint.actions(), playState);
            }

            updatePlayerFromState(world, player, playState, isStarting);

            // If this is OS, reset the player as they are added
            if (world.map().getSetting(MapSettings.ONLY_SPRINT) && !player.getTag(RESET_TAG) && event.isFirstInit()) {
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

    public void handleCheckpointPostChange(@NotNull MapPlayerCheckpointPostChangeEvent event) {
        var player = event.getPlayer();

        // Save state can be missing when the player enters spectator mode while standing on a checkpoint.
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) return;

        var state = saveState.state(PlayState.class);
        var data = event.effectData();

        if (!event.getMapWorld().isPlaying(player)) return;
        if (state.history().isEmpty()) return;
        if (state.lastState() == null) return;
        if (!state.history().getLast().equals(event.checkpointId())) return;
        if (data.actions().has(TeleportAction.KEY)) return;
        var pos = state.pos();
        if (pos == null) return;
        float yaw = player.getPosition().yaw();
        float pitch = player.getPosition().pitch();

        state.setPos(pos.withView(yaw, pitch));
        OpUtils.build(state.lastState(), s -> s.setPos(OpUtils.map(s.pos(), p -> p.withView(yaw, pitch))));
    }

    public void handleCheckpointChange(@NotNull MapPlayerCheckpointPreChangeEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayer(player);
        var state = SaveState.fromPlayer(player).state(PlayState.class);
        var data = event.effectData();

        // Ensure the event should trigger a checkpoint change for the current players state
        if (checkProgressIndex(player, world, state, data.actions())) return;
        if (state.lastState() != null && state.lastState().hasStatus(event.checkpointId()))
            return; // Player already has this checkpoint in their history (they are backtracking)

        // Apply the checkpoint/effect changes
        state.set(EditTimerAction.SAVE_DATA, null); // Time always reset on checkpoint
        player.removeTag(COUNTDOWN_END);
        updateStateFromPlayer(player, state);
        updateBaseEffectState(world, player, data.actions(), state);

        // The checkpoint (reset) pos is set to the teleport if its present, or the first
        // position the player touched the checkpoint otherwise. todo probably need to do a gravity snap here
        // to bring it down to the ground.
        var checkpointPos = player.getPosition();

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
                null,
                newHistory,
                checkpointPos,
                Map.copyOf(state.ghostBlocks()),
                Map.copyOf(state.actionData())
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
        if (checkProgressIndex(player, world, state, data.actions())) return;

        // Apply the status changes
        updateStateFromPlayer(player, state);
        updateBaseEffectState(world, player, data.actions(), state);
        state.addStatus(event.statusId());
//        state.settings().update(data.settings());

        // Update the player based on the new state
        updatePlayerFromState(world, player, state);
    }

    private boolean checkProgressIndex(@NotNull Player player, @NotNull MapWorld world, @NotNull PlayState state, @NotNull ActionList actionList) {
        int progressIndex = OpUtils.mapOr(actionList.findLast(SetProgressIndexAction.class), SetProgressIndexAction::value, -1);
        if (progressIndex > 0) {
            int currentIndex = state.get(Attachments.PROGRESS_INDEX, 0);
            boolean condition = world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION)
                    // With additive index you can get anything <= current + 1
                    ? (progressIndex > currentIndex + 1)
                    // Without additive progress index you must be at the prior index or the current one
                    : (progressIndex != currentIndex && progressIndex != currentIndex + 1);
            if (condition) {
                if (PROGRESS_INDEX_WARNING.test(player)) {
                    player.sendMessage(Component.translatable("checkpoint.progress_index.not_acceptable",
                            Component.text(currentIndex), Component.text(progressIndex - 1)));
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

    private void handleInitTimerFromMove(@NotNull Player player, @NotNull Pos newPos) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return;

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null || saveState.getPlayStartTime() != 0) return;

        var oldPosition = player.getPosition();
        if (Vec.fromPoint(oldPosition).equals(Vec.fromPoint(newPos)))
            return; // Player did not actually move, just turn their head

        // Start the timer.
        saveState.setPlayStartTime(System.currentTimeMillis());

        var timer = saveState.state(PlayState.class).get(EditTimerAction.SAVE_DATA);
        if (timer != null && timer > 0) {
            player.setTag(COUNTDOWN_END, System.currentTimeMillis() + (timer * 50L));
        }
    }

    public void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        if (world.isPlaying(player)) {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null) return;

            var playState = saveState.tryGetState(PlayState.class).orElse(null);
            if (playState == null) return;
            var resetHeight = Objects.requireNonNullElseGet(playState.get(Attachments.RESET_HEIGHT),
                    () -> world.instance().getTag(DEFAULT_RESET_HEIGHT));
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
                var checkpoint = SpectateHandler.getCheckpoint(player);
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

        saveState.uncomplete();
        saveState.setPlaytime(0);
        saveState.setPlayStartTime(0);
        var newPlayState = new PlayState();
        saveState.setState(newPlayState);

        SpectateHandler.setCheckpoint(player, null);
        player.removeTag(COUNTDOWN_END);

        resetTeleport(player, world.map().settings().getSpawnPoint()).thenRun(() -> {
            try {
                updatePlayerFromState(world, player, newPlayState, true);
                abstractWorld.addPlayerImmediate(player);

                EventDispatcher.call(new MapPlayerInitEvent(world, player, true, false));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
            }
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
        var checkpoint = SpectateHandler.getCheckpoint(player);
        if (checkpoint != null && (world.isSpectating(player) || world instanceof TestingMapWorld)) {
            // If the checkpoint is below the reset height, teleport to the spawn instead to prevent getting stuck.
            // If they set the spawn below the world then its a joke map anyway and i don't care.
            var playState = saveState.state(PlayState.class);
            var resetHeight = Objects.requireNonNullElseGet(playState.get(Attachments.RESET_HEIGHT),
                    () -> world.instance().getTag(DEFAULT_RESET_HEIGHT));
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
        var isOutOfLives = OpUtils.mapOr(playState.get(EditLivesAction.SAVE_DATA), EditLivesAction.Data::value, 0) == 1;
        if (playState.lastState() == null || isOutOfLives) {
            if (isOutOfLives) {
                player.playSound(PLAYER_DEATH_SOUND);
            }
            hardReset(player, saveState);
            return;
        }

        // Remove the playing tag so that they can't trigger a checkpoint/status/completion
        abstractWorld.removePlayerImmediate(player);

        // "pop" the last state to the current
        playState = playState.lastState();
        var lives = playState.get(EditLivesAction.SAVE_DATA);
        if (lives != null) {
            // This is definitely valid, we checked above to see if this was the last life.
            playState.set(EditLivesAction.SAVE_DATA, lives.withValue(lives.value() - 1));
            player.playSound(PLAYER_HURT_SOUND);
        }
        saveState.setState(playState);
        // Create a copy so that we can reset to the checkpoint again
        playState.setLastState(playState.copy());

        player.removeTag(COUNTDOWN_END); // Remove so it is reapplied by updatePlayerFromState
        // Apply the current state to the player and teleport them
        updatePlayerFromState(world, player, playState);
        resetTeleport(player, Objects.requireNonNull(playState.pos())).thenRun(() -> {
            abstractWorld.addPlayerImmediate(player);

            EventDispatcher.call(new MapPlayerInitEvent(world, player, false, false));
        });
    }

    private void updateBaseEffectState(@NotNull MapWorld world, @NotNull Player player, @NotNull ActionList actions, @NotNull PlayState state) {
        for (var action : actions.actions()) {
            action.applyTo(player, state);
        }

//        var newItem1 = state.items().item1();
//        if (data.items().item1() != null) newItem1 = data.items().item1();
//        var newItem2 = state.items().item2();
//        if (data.items().item2() != null) newItem2 = data.items().item2();
//        var newItem3 = state.items().item3();
//        if (data.items().item3() != null) newItem3 = data.items().item3();
//        var newElytra = state.items().elytra();
//        if (data.items().elytra() != null) newElytra = data.items().elytra();
//
//        state.setItems(new HotbarItems(newItem1, newItem2, newItem3, newElytra));
    }

    private void updateStateFromPlayer(@NotNull Player player, @NotNull PlayState state) {
        long now = System.currentTimeMillis();

        var countdownEnd = player.getTag(COUNTDOWN_END);
        if (countdownEnd != -1) {
            // We have to clamp it to 1 because if we don't when they rejoin their time limit will
            // be less than or equal to 0 meaning it will allow them to play forever due tp <= 0 being infinite time.
            state.set(EditTimerAction.SAVE_DATA, (int) Math.max((countdownEnd - now) / 50, 1));
        }

        // Update remaining time for the remaining effects (and remove if expired)
        var iter = state.get(Attachments.POTION_EFFECTS, new PotionEffectList()).entries().iterator();
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

//        var items = state.items();
//        state.setItems(new HotbarItems(
//                items.item1() == null ? HotbarItem.Remove.INSTANCE : items.item1().fromItemStack(player.getInventory().getItemStack(3)),
//                items.item2() == null ? HotbarItem.Remove.INSTANCE : items.item2().fromItemStack(player.getInventory().getItemStack(5)),
//                items.item3() == null ? HotbarItem.Remove.INSTANCE : items.item3().fromItemStack(player.getInventory().getItemStack(6)),
//                items.elytra()
//        ));
    }

    private void updatePlayerFromState(MapWorld world, @NotNull Player player, @NotNull PlayState state) {
        updatePlayerFromState(world, player, state, false);
    }

    private void updatePlayerFromState(MapWorld world, @NotNull Player player, @NotNull PlayState state, boolean start) {
        // Set the player health to the number of time they have (1 heart = 1 life)
        var lives = state.get(EditLivesAction.SAVE_DATA);
        if (lives != null) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2 * lives.max());
            player.setHealth(2 * lives.value());
        } else {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Attribute.MAX_HEALTH.defaultValue());
            player.setHealth((float) Attribute.MAX_HEALTH.defaultValue());
        }

        // Update the countdown timer (time may have been added
        var timeLimit = state.get(EditTimerAction.SAVE_DATA);
        if (timeLimit != null && !start) {
            player.setTag(COUNTDOWN_END, System.currentTimeMillis() + (timeLimit * 50));
        } else {
            player.removeTag(COUNTDOWN_END);
        }

        // Update the potions on the player
        player.clearEffects();
        for (var entry : state.get(Attachments.POTION_EFFECTS, new PotionEffectList()).entries()) {
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
//        var item1 = state.items().item1();
//        player.getInventory().setItemStack(3, item1 == null || item1 instanceof HotbarItem.Remove
//                ? ItemStack.AIR : item1.toItemStack(false));
//        var item2 = state.items().item2();
//        player.getInventory().setItemStack(5, item2 == null || item2 instanceof HotbarItem.Remove
//                ? ItemStack.AIR : item2.toItemStack(false));
//        var item3 = state.items().item3();
//        player.getInventory().setItemStack(6, item3 == null || item3 instanceof HotbarItem.Remove
//                ? ItemStack.AIR : item3.toItemStack(false));

        if (state.get(Attachments.ELYTRA, false)) {
            player.setChestplate(player.getChestplate().with(DataComponents.GLIDER)
                    .with(DataComponents.EQUIPPABLE, ELYTRA_EQUIPPABLE));
        } else {
            player.setChestplate(player.getChestplate().without(DataComponents.GLIDER)
                    .with(DataComponents.EQUIPPABLE, EMPTY_EQUIPPABLE));
            if (player.isFlyingWithElytra()) player.setFlyingWithElytra(false);
        }

        world.callEvent(new MapPlayerUpdateStateEvent(world, player));
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

    private static CompletableFuture<Void> resetTeleport(@NotNull Player player, @NotNull Pos position) {
        FireworkRocketItem.removeRocket(player);
        player.getPlayerMeta().setFlyingWithElytra(false);
        player.getInstance().eventNode().call(new PlayerStopFlyingWithElytraEvent(player));
        return player.teleport(position, Vec.ZERO, null, RelativeFlags.NONE);
    }

}

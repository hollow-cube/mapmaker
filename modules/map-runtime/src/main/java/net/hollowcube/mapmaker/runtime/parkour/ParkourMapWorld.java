package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.events.PlayerMoveVehicleEvent;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.cosmetic.impl.victory.AbstractVictoryEffectImpl;
import net.hollowcube.mapmaker.gui.map.RateMapView;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandlerRegistry;
import net.hollowcube.mapmaker.map.entity.potion.PotionHandler;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.item.vanilla.*;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.util.MapCompletionAnimation;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.AppliedRewards;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.HotbarItems;
import net.hollowcube.mapmaker.runtime.parkour.action.LegacyActionStateManager;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditLivesAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.EditTimerAction;
import net.hollowcube.mapmaker.runtime.parkour.block.*;
import net.hollowcube.mapmaker.runtime.parkour.hud.ParkourDebugHud;
import net.hollowcube.mapmaker.runtime.parkour.hud.ResetHeightDisplay;
import net.hollowcube.mapmaker.runtime.parkour.item.*;
import net.hollowcube.mapmaker.runtime.parkour.marker.*;
import net.hollowcube.mapmaker.runtime.parkour.marker.bouncepad.BouncePadMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.setting.*;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;
import static net.kyori.adventure.text.Component.translatable;

public class ParkourMapWorld extends AbstractMapWorld<ParkourState, ParkourMapWorld> {

    private static final int RESET_HEIGHT_OFFSET = 5;

    private static final Sound PLAYER_HURT_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_HURT, Sound.Source.PLAYER, 1, 1f);
    private static final Sound PLAYER_DEATH_SOUND = Sound.sound(SoundEvent.ENTITY_PLAYER_DEATH, Sound.Source.PLAYER, 1, 1f);

    private static final Tag<@Nullable Long> BEST_PLAYTIME = Tag.Transient("map:best_playtime");

    private static final List<ItemHandler> SILENT_ITEMS = List.of(
            // Hotbar items
            MapDetailsItem.INSTANCE, ReturnToHubItem.INSTANCE, RateMapItem.INSTANCE,
            ReturnToCheckpointItem.INSTANCE, ResetSaveStateItem.INSTANCE,
            ToggleSpectatorModeItem.INSTANCE_OFF, ToggleSpectatorModeItem.INSTANCE_ON,
            ToggleGameplayItem.INSTANCE_OFF, ToggleGameplayItem.INSTANCE_ON,
            ToggleFlightItem.INSTANCE_OFF, ToggleFlightItem.INSTANCE_ON,
            SetSpectatorCheckpointItem.INSTANCE,
            // Gameplay items
            FireworkRocketItem.INSTANCE, EnderPearlItem.INSTANCE,
            WindChargeItem.INSTANCE, TridentItem.INSTANCE,
            MaceItem.INSTANCE
    );

    // Holds the CheckpointEffectData applied to the player on first spawn.
    public static final Tag<ActionTriggerData> SPAWN_CHECKPOINT_EFFECTS = DFU.Tag(ActionTriggerData.CODEC, "spawn_checkpoint_effects");

    private static @Nullable ServerProcess initProcess = null;

    public static @Nullable ParkourMapWorld forPlayer(Player player) {
        return MapWorld.forPlayer(ParkourMapWorld.class, player);
    }

    public static void initGlobalReferences() {
        var process = MinecraftServer.process();
        if (initProcess == process) return;
        initProcess = process;

        // Force init. Very gross should fix
        var _ = Attachments.PROGRESS_INDEX;
        var _ = HotbarItems.CODEC;
        var _ = ActionTriggerData.CODEC;

        process.packetListener().setPlayListener(
                ClientPlayerBlockPlacementPacket.class,
                ClientBlockPlacementListener::handleBlockPlacementPacket);

        process.block().registerHandler(BouncePadBlock.KEY, BouncePadBlock::new); // Not stateless
        process.block().registerHandler(CheckpointPlateBlock.INSTANCE.getKey(), () -> CheckpointPlateBlock.INSTANCE);
        process.block().registerHandler(FinishPlateBlock.INSTANCE.getKey(), () -> FinishPlateBlock.INSTANCE);
        process.block().registerHandler(StatusPlateBlock.INSTANCE.getKey(), () -> StatusPlateBlock.INSTANCE);

        process.block().registerHandler(DripleafBlock.INSTANCE.getKey(), () -> DripleafBlock.INSTANCE);

        MinecraftServer.getGlobalEventHandler().addChild(PotionHandler.EVENT_NODE);
    }

    public static void registerMarkers(ObjectEntityHandlerRegistry objectEntityHandlers) {
        objectEntityHandlers.registerForMarkers(MapLeaderboardMarkerHandler.ID, MapLeaderboardMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(BouncePadMarkerHandler.ID, BouncePadMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(HappyGhastMarkerHandler.ID, HappyGhastMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(CheckpointMarkerHandler.ID, CheckpointMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(StatusMarkerHandler.ID, StatusMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(FinishMarkerHandler.ID, FinishMarkerHandler::new);
        objectEntityHandlers.registerForMarkers(ResetMarkerHandler.ID, ResetMarkerHandler::new);
    }

    private final SaveStateType saveStateType;

    protected int defaultResetHeight;

    public ParkourMapWorld(MapServer server, MapData map) {
        this(server, map, makeMapInstance(map, 'p'));
    }

    protected ParkourMapWorld(MapServer server, MapData map, MapInstance instance) {
        super(server, map, instance, ParkourState.class);
        Check.stateCondition(initProcess == null, "ParkourMapWorld is not initialized, was `ParkourMapWorld2.initGlobalReferences()` called?");

        this.saveStateType = map.verification() == MapVerification.PENDING
                ? SaveStateType.VERIFYING : SaveStateType.PLAYING;
        this.defaultResetHeight = instance().getCachedDimensionType().minY();

        SILENT_ITEMS.forEach(itemRegistry()::registerSilent);

        registerMarkers(objectEntityHandlers());

        eventNode(ParkourState.AnyPlaying.class)
                .addListener(PlayerMoveEvent.class, event -> handlePlayerOrVehicleMove(event.getPlayer(), event.getNewPosition()))
                .addListener(PlayerMoveVehicleEvent.class, event -> handlePlayerOrVehicleMove(event.getPlayer(), event.getNewPosition()))
                .addListener(PlayerTickEvent.class, this::handlePlayerTick)
                .addChild(DelayedBlockInteractions.EVENT_NODE)
                .addChild(LegacyActionStateManager.EVENT_NODE)
                .addChild(ResetHeightDisplay.EVENT_NODE)
                .addChild(DoubleJumpSetting.EVENT_NODE)
                .addChild(NoJumpSetting.EVENT_NODE)
                .addChild(NoRelogSetting.EVENT_NODE)
                .addChild(NoSneakSetting.EVENT_NODE)
                .addChild(NoSprintSetting.EVENT_NODE)
                .addChild(NoTurnSetting.EVENT_NODE)
                .addChild(OnlySprintSetting.EVENT_NODE)
                .addChild(ResetLiquidSetting.EVENT_NODE)
                .addChild(TickRateSetting.EVENT_NODE);

        eventNode(ParkourState.Spectating.class)
                .addListener(PlayerMoveEvent.class, this::handleSpectatorMove);

        eventNode(ParkourState.Finished.class)
                .addListener(PlayerMoveEvent.class, this::handleSpectatorMove);

        // Make the entire world readonly to all players inside it (spec or playing doesn't matter)
        eventNode().addChild(EventUtil.READ_ONLY_NODE);

        scheduler().submitTask(this::visibilityTick);
    }

    public int defaultResetHeight() {
        return defaultResetHeight;
    }

    // region Player Lifecycle

    /// Creates the expected playing state type for this map. Mostly exists for testing world.
    public ParkourState.AnyPlaying createPlayingState(SaveState saveState) {
        return new ParkourState.Playing2(saveState);
    }

    public void hardResetPlayer(Player player) {
        var newSaveState = new SaveState(UUID.randomUUID().toString(),
                map().id(), player.getUuid().toString(), saveStateType,
                PlayState.SERIALIZER, new PlayState());
        newSaveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        changePlayerState(player, createPlayingState(newSaveState));
    }

    public void softResetPlayer(Player player) {
        if (!(getPlayerState(player) instanceof ParkourState.AnyPlaying playing))
            return;
        var saveState = playing.saveState();
        var playState = saveState.state(PlayState.class);

        // If you either: have no prior checkpoint, or have no more lives
        // then you will be hard reset instead (or lives special behavior potentially).
        var livesData = playState.get(EditLivesAction.SAVE_DATA);
        // We do less than or equals 1 because if an action subtracts lives to 0 or below, we will call softResetPlayer
        var isOutOfLives = livesData != null && livesData.value() <= 1;
        var newPlayState = OpUtils.map(playState.lastState(), PlayState::copy);
        if (newPlayState == null || isOutOfLives) {
            if (isOutOfLives) {
                player.playSound(PLAYER_DEATH_SOUND);
                player.sendMessage(translatable("playing.lives.run_out"));

                // Special handling for lives death position teleportation.
                // The player gets an entirely wiped play state, but does _not_ otherwise
                // reset their save state.
                if (livesData.deathPosition() != null) {
                    var resetPlayState = new PlayState();
                    resetPlayState.setPos(livesData.deathPosition().resolve(player.getPosition()));
                    changePlayerState(player, playing.withSaveState(saveState.copy(resetPlayState)));
                    return;
                }
            }

            hardResetPlayer(player);
            return;
        }

        // Decrement remaining lives
        var lives = newPlayState.get(EditLivesAction.SAVE_DATA);
        if (lives != null) {
            // This is definitely valid, we checked above to see if this was the last life.
            newPlayState.set(EditLivesAction.SAVE_DATA, lives.withValue(lives.value() - 1));
            player.playSound(PLAYER_HURT_SOUND);
        }

        // Create a copy so that we can reset to this checkpoint again
        newPlayState.setLastState(newPlayState.copy());

        // Resume playing from this state as a safe point action
        var newSaveState = saveState.copy(newPlayState);
        changePlayerState(player, playing.withSaveState(newSaveState));
    }

    @Override
    public ParkourState configurePlayer(Player player) {
        final var playerData = PlayerData.fromPlayer(player);
        SaveState saveState;
        try {
            saveState = server().mapService().getLatestSaveState(map().id(),
                    playerData.id(), saveStateType, PlayState.SERIALIZER);
        } catch (MapService.NotFoundError ignored) {
            // No save state yet, create one locally.
            // We do an upsert to save, so it will be created in the map service at that point.
            saveState = new SaveState(UUID.randomUUID().toString(),
                    map().id(), playerData.id(), saveStateType,
                    PlayState.SERIALIZER, new PlayState());
            saveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        }

        player.setRespawnPoint(Objects.requireNonNullElseGet(
                saveState.state(PlayState.class).pos(),
                () -> map().settings().getSpawnPoint()
        ));

        if (RateMapItem.isMapRatable(this)) {
            RateMapItem.initLastRating(server().mapService(), player, map());
        }

        player.setTag(BEST_PLAYTIME, OpUtils.map(
                server().mapService().getBestSaveState(map().id(), player.getUuid().toString()),
                SaveState::getPlaytime
        ));

        return createPlayingState(saveState);
    }

    @Override
    public void removePlayer(Player player) {
        // Always try to remove, feature flag could have been removed since entering world and we still want it gone
        ActionBar.forPlayer(player).removeProvider(ParkourDebugHud.INSTANCE);

        player.removeTag(BEST_PLAYTIME);

        super.removePlayer(player);
    }

    @Override
    @Deprecated
    public boolean shouldTriggerDripleaf(Player player) {
        return switch (getPlayerState(player)) {
            case ParkourState.Spectating _, ParkourState.Finished _ -> false;
            case ParkourState.Playing2 _, ParkourState.Testing _ -> true;
            case null -> false;
        };
    }

    /// Initializes the parkour timer from some action the player took
    /// for example moving or placing a block
    public void initTimerFromAction(Player player) {
        if (!(getPlayerState(player) instanceof ParkourState.AnyPlaying playing))
            return;
        initTimerFromAction(player, playing.saveState());
    }

    /// Initializes the parkour timer from some action the player took
    /// for example moving or placing a block
    public void initTimerFromAction(Player player, SaveState saveState) {
        if (saveState.getPlayStartTime() != 0) return;

        // Start the timer.
        saveState.setPlayStartTime(System.nanoTime() / 1_000_000);
        // Reset touching state so you can begin touching
        ((MapPlayer) player).resetTouchingState();

        var timer = saveState.state(PlayState.class).get(EditTimerAction.SAVE_DATA);
        if (timer != null && timer > 0) {
            player.setTag(EditTimerAction.COUNTDOWN_END, System.nanoTime() / 1_000_000 + (timer * 50L));
        }
    }

    private void handlePlayerOrVehicleMove(Player player, Pos newPos) {
        if (!(getPlayerState(player) instanceof ParkourState.AnyPlaying playing))
            return;

        var saveState = playing.saveState();
        var oldPosition = player.getPosition();
        if (!oldPosition.samePoint(newPos)) {
            initTimerFromAction(player, saveState);
        }

        var playState = saveState.state(PlayState.class);
        int resetHeight = OpUtils.or(playState.get(Attachments.RESET_HEIGHT), this::defaultResetHeight);
        if (player.getPosition().y() < resetHeight) {
            softResetPlayer(player);
            return;
        }
    }

    private void handlePlayerTick(PlayerTickEvent event) {
        final var player = event.getPlayer();

        var countdownEnd = player.getTag(EditTimerAction.COUNTDOWN_END);
        if (countdownEnd != -1 && countdownEnd < System.nanoTime() / 1_000_000) {
            player.sendMessage(translatable("playing.timer.run_out"));
            softResetPlayer(player);
        }

        if (getPlayerState(player) instanceof ParkourState.AnyPlaying playing) {
            playing.saveState().tick();
        }
    }

    private void handleSpectatorMove(PlayerMoveEvent event) {
        if (event.getNewPosition().y() < instance().getCachedDimensionType().minY()) {
            ParkourState.AnyPlaying.resetTeleport(event.getPlayer(), map().settings().getSpawnPoint());
        }
    }

    public void performFinishEffects(Player player, SaveState finishState) {
        // Show the completed message after removing the player because it is theoretically possible to not have the savestate fetched yet.
        Long bestPlaytime = player.getTag(BEST_PLAYTIME);
        if (bestPlaytime == null) {
            player.setTag(BEST_PLAYTIME, finishState.getPlaytime());
            player.sendMessage(Component.translatable(
                    "map.completed.first",
                    Component.text(formatMapPlaytime(finishState.getPlaytime(), true))
            ));
        } else {
            // Diff playtime rounded to ticks prior to subtracting for correct display.
            var diffPlaytime = NumberUtil.roundMillisToTicks(bestPlaytime) -
                    NumberUtil.roundMillisToTicks(finishState.getPlaytime());
            var diffColor = diffPlaytime < 0 ? NamedTextColor.RED : NamedTextColor.GREEN;
            var diffSymbol = diffPlaytime < 0 ? "+" : "-";
            player.sendMessage(Component.translatable(
                    "map.completed.with_prior",
                    Component.text(formatMapPlaytime(finishState.getPlaytime(), true)),
                    // Note: roundToTicks is not used here. We do the rounding above because we need to round prior to calculating the difference.
                    Component.text(diffSymbol + formatMapPlaytime(Math.abs(diffPlaytime), false), diffColor)
            ));

            if (finishState.getPlaytime() < bestPlaytime) {
                player.setTag(BEST_PLAYTIME, finishState.getPlaytime());
            }
        }

        // Will be called when the completion animation is finished
        var lastRatingFuture = player.getTag(RateMapItem.LAST_RATING_TAG);
        final Runnable tryShowRateGui = () -> {
            if (RateMapItem.isMapRatable(this) && lastRatingFuture.state() == Future.State.SUCCESS) {
                final MapRating lastRating = lastRatingFuture.resultNow();
                // Note that we dont want to open the GUI if you have since opened a different inventory because its really annoying in practice.
                if ((lastRating == null || lastRating.state() == MapRating.State.UNRATED) && player.getOpenInventory() == null) {
                    Panel.open(player, new RateMapView(server().mapService(), map(), MapRating.State.UNRATED, newState ->
                            player.setTag(RateMapItem.LAST_RATING_TAG, CompletableFuture.completedFuture(new MapRating(newState, null)))));
                }
            }
        };

        // Show the completion animation
        MapCompletionAnimation.schedule(player, new AppliedRewards.Inventory(null, null, null, null), tryShowRateGui);

        // Play the victory effect
        var playerData = PlayerData.fromPlayer(player);
        var victoryEffect = Cosmetic.byId(CosmeticType.VICTORY_EFFECT, playerData.getSetting(CosmeticType.VICTORY_EFFECT.setting()));
        if (victoryEffect != null && victoryEffect.impl() instanceof AbstractVictoryEffectImpl impl) {
            impl.trigger(player, player.getPosition());
        }
    }

    public void handleTestingModeFinish(Player player) {
        // Noop except in testparkourmapworld. pretty gross but i dont have a better solution immediately.
    }

    private TaskSchedule visibilityTick() {
        for (var player : players()) {
            player.updateViewerRule(); // Spec doesnt currently have a viewer rule
            if (player instanceof MapPlayer mp)
                mp.updateVisibility();
        }
        return TaskSchedule.tick(5);
    }

    public @Nullable Long getPlayerBestPlaytime(Player player) {
        return player.getTag(BEST_PLAYTIME);
    }

    // endregion

    //region World lifecycle

    @Override
    public void loadWorld() {
        super.loadWorld();

        computeDefaultResetHeight();
    }

    @Override
    public void loadWorldTag(TagReadable tag) {
        super.loadWorldTag(tag);

        instance().setTag(SPAWN_CHECKPOINT_EFFECTS, tag.getTag(SPAWN_CHECKPOINT_EFFECTS));
    }

    @Override
    public void saveWorldTag(TagWritable tag) {
        super.saveWorldTag(tag);

        tag.setTag(SPAWN_CHECKPOINT_EFFECTS, instance().getTag(SPAWN_CHECKPOINT_EFFECTS));
    }

    protected void computeDefaultResetHeight() {
        int worldMinHeight = instance().getCachedDimensionType().minY();
        int minBlockY = instance().getCachedDimensionType().maxY();

        int worldRadius = (int) (instance().getWorldBorder().diameter() / 2) + 16;
        worldRadius = Math.min(worldRadius, 4096); // Prevent infinite computation
        for (int x = -worldRadius; x < worldRadius; x += 16) {
            for (int z = -worldRadius; z < worldRadius; z += 16) {
                var rawChunk = instance().getChunkAt(x, z);
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
        if (minBlockY == instance().getCachedDimensionType().maxY()) minBlockY = worldMinHeight;
        this.defaultResetHeight = minBlockY - RESET_HEIGHT_OFFSET;
    }

    @Override
    protected @Nullable List<BossBar> createBossBars() {
        return BossBars.createPlayingBossBar(server().playerService(), map());
    }

    //endregion

}

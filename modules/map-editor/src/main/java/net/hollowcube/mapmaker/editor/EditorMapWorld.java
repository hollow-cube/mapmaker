package net.hollowcube.mapmaker.editor;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.editor.command.navigation.BackCommand;
import net.hollowcube.mapmaker.editor.entity.SpawnMarkerEntity;
import net.hollowcube.mapmaker.editor.item.BuilderMenuItem;
import net.hollowcube.mapmaker.editor.item.EnterTestModeItem;
import net.hollowcube.mapmaker.editor.item.ExitTestModeItem;
import net.hollowcube.mapmaker.editor.item.SpawnPointItem;
import net.hollowcube.mapmaker.editor.parkour.BouncePadEditor;
import net.hollowcube.mapmaker.editor.parkour.CheckpointEditor;
import net.hollowcube.mapmaker.editor.parkour.FinishEditor;
import net.hollowcube.mapmaker.editor.parkour.StatusEditor;
import net.hollowcube.mapmaker.editor.terraform.TerraformInstanceStorageImpl;
import net.hollowcube.mapmaker.editor.vanilla.DisplayEntityEditor;
import net.hollowcube.mapmaker.editor.vanilla.PickBlock;
import net.hollowcube.mapmaker.editor.vanilla.SignEditor;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEditorScreen;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.event.MapPlayerTeleportingEvent;
import net.hollowcube.mapmaker.map.item.vanilla.DebugStickItem;
import net.hollowcube.mapmaker.map.polar.ReadWriteWorldAccess;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorView;
import net.hollowcube.mapmaker.runtime.parkour.marker.CheckpointMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.marker.FinishMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.marker.StatusMarkerHandler;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.instance.TerraformInstanceBiomes;
import net.hollowcube.terraform.storage.TerraformInstanceStorage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerPickBlockEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.BlockingExecutor;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld.SPAWN_CHECKPOINT_EFFECTS;

public class EditorMapWorld extends AbstractMapWorld<EditorState, EditorMapWorld> {
    private static final Logger logger = LoggerFactory.getLogger(EditorMapWorld.class);

    private static final int MAP_AUTOSAVE_INTERVAL_SEC = ServerRuntime.getRuntime().isDevelopment() ? 60 : 5 * 60;
    private static final int WORLD_BORDER_WARNING_DISTANCE = 5;

    public static @Nullable EditorMapWorld forPlayer(Player player) {
        return MapWorld.forPlayer(EditorMapWorld.class, player);
    }

    private final SpawnMarkerEntity spawnEntity;

    private final ReentrantLock saveLock = new ReentrantLock();
    private @Nullable Task autoSaveTask = null;

    private final ReentrantLock testWorldLock = new ReentrantLock();
    private @Nullable AbstractMapWorld<?, ?> testWorld;

    private final @Nullable Terraform terraform;
    private final @Nullable TerraformInstanceStorage terraformInstanceStorage;

    public EditorMapWorld(MapServer server, MapData map, @Nullable Terraform terraform) {
        super(server, map, makeMapInstance(map, 'e'), EditorState.class);

        this.spawnEntity = new SpawnMarkerEntity();
        instance().scheduleNextTick(_ -> this.spawnEntity.setInstance(instance(), map().settings().getSpawnPoint()));

        itemRegistry().register(BuilderMenuItem.INSTANCE);
        itemRegistry().register(DebugStickItem.INSTANCE);
        itemRegistry().register(SpawnPointItem.INSTANCE);
        itemRegistry().register(EnterTestModeItem.INSTANCE);
        itemRegistry().register(ExitTestModeItem.INSTANCE);

        itemRegistry().register(CheckpointEditor.PLATE_ITEM);
        itemRegistry().register(StatusEditor.PLATE_ITEM);
        itemRegistry().register(FinishEditor.PLATE_ITEM);
        itemRegistry().register(BouncePadEditor.ITEM);

        ParkourMapWorld.registerMarkers(objectEntityHandlers());
        objectEntityHandlers().registerEditor(CheckpointMarkerHandler.ID, CheckpointEditor.MARKER_EDITOR);
        objectEntityHandlers().registerEditor(StatusMarkerHandler.ID, StatusEditor.MARKER_EDITOR);
        objectEntityHandlers().registerEditor(FinishMarkerHandler.ID, FinishEditor.MARKER_EDITOR);
        objectEntityHandlers().registerDefaultEditor(InteractionEntity.class, InteractionEditorScreen.MARKER_EDITOR);

        eventNode(EditorState.Building.class)
            .addListener(PlayerTickEvent.class, this::handlePlayerTick)
            .addListener(MapPlayerTeleportingEvent.class, this::handlePlayerSavedTeleport)
            .addListener(PlayerEntityInteractEvent.class, this::handleSpawnEntityInteraction)
            .addListener(PlayerPickBlockEvent.class, PickBlock::handlePickBlock)
            .addChild(SignEditor.EVENT_NODE)
            .addChild(CheckpointEditor.EVENT_NODE)
            .addChild(StatusEditor.EVENT_NODE);

        eventNode().addChild(DisplayEntityEditor.EVENT_NODE);

        // Terraform initialization
        this.terraform = terraform;
        if (terraform != null) {
            terraformInstanceStorage = new TerraformInstanceStorageImpl();
            instance().setTag(TerraformInstanceBiomes.BIOMES, biomes());
            instance().setTag(TerraformInstanceStorage.TERRAFORM_INSTANCE_STORAGE_TAG, terraformInstanceStorage);
        } else {
            terraformInstanceStorage = null;
        }
    }

    public Terraform terraform() {
        return Objects.requireNonNull(terraform);
    }

    @Override
    public boolean canEdit(@Nullable Player player) {
        return player == null || getPlayerState(player) instanceof EditorState.Building;
    }

    /// Changes the map spawn point
    ///
    /// Note that the validity is not checked, you could set the spawn outside the world border if not careful.
    public void setSpawnPoint(Pos newSpawnPoint) {
        map().settings().setSpawnPoint(newSpawnPoint);

        spawnEntity.setView(newSpawnPoint.yaw(), newSpawnPoint.pitch());
        spawnEntity.teleport(newSpawnPoint);
    }

    //region World Lifecycle

    @Override
    protected void configureInstance() {
        super.configureInstance();

        // Warning distance creates the red border when nearby the world border.
        instance().setWorldBorder(instance().getWorldBorder()
            .withWarningTime(WORLD_BORDER_WARNING_DISTANCE)
            .withWarningDistance(WORLD_BORDER_WARNING_DISTANCE));
    }

    @Override
    public void loadWorld() {
        super.loadWorld();

        // Kick off autosave
        if (map().verification() == MapVerification.UNVERIFIED) {
            autoSaveTask = instance().scheduler().buildTask(FutureUtil.wrapVirtual(() -> save(true)))
                .delay(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                .repeat(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                .schedule();
        }
    }

    @Override
    protected @Nullable List<BossBar> createBossBars() {
        return BossBars.createEditingBossBar(map());
    }

    @Override
    public void loadWorldTag(TagReadable tag) {
        super.loadWorldTag(tag);

        if (terraformInstanceStorage != null) terraformInstanceStorage.load(tag);
        instance().setTag(SPAWN_CHECKPOINT_EFFECTS, tag.getTag(SPAWN_CHECKPOINT_EFFECTS));
    }

    @Override
    public void close() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        autoSaveTask = null;

        testWorldLock.lock();
        try {
            testWorld = null;
        } finally {
            testWorldLock.unlock();
        }

        FutureUtil.submitVirtual(() -> {
            save(false);

            MinecraftServer.getSchedulerManager()
                .scheduleEndOfTick(super::close);
        });
    }

    @Blocking
    private void save(boolean isAutoSave) {
        saveLock.lock();
        try {
            if (isAutoSave) logger.info("Autosaving world {}", map().id());
            if (!isAutoSave) logger.info("Manually saving world {}", map().id());

            // Save the map settings
            map().settings().withUpdateRequest(updates -> {
                try {
                    //todo map worlds should have an "owner" which is the owner if they are present, otherwise
                    // it is the first trusted player to join the world. If someone leaves it should find a new
                    // "owner" of the world, or if it cannot (only invited people left), it should close the world.
                    server().mapService().updateMap(map().owner(), map().id(), updates);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to save map settings for {}", map().id(), e);
                    ExceptionReporter.reportException(e);
                    return false;
                }
            });

            // Save the world data (if it is unverified only)
            if (map().verification() != MapVerification.PENDING) {
                var worldData = instance().save(new ReadWriteWorldAccess(this));
                server().mapService().updateMapWorld(map().id(), worldData);
            }

            for (var player : Set.copyOf(players())) {
                // The following is a sanity check which should be removed. It almost certainly
                // indicates a bug if it ever happens, so report it to posthog.
                if (!player.isOnline()) {
                    ExceptionReporter.reportException(new IllegalStateException("player is not online during save"), player);
                    // Its kinda weird to remove someone we know is offline but they are somehow still in the map so we need them gone.
                    scheduleRemovePlayer(player);
                    continue;
                }

                savePlayerState(player, false);
            }

            if (isAutoSave) instance().sendMessage(Component.translatable("build.world.save.success"));
        } catch (Exception e) {
            ExceptionReporter.reportException(new RuntimeException("failed to save world: " + map().id(), e));
            instance().sendMessage(Component.translatable("build.world.save.failure"));
        } finally {
            saveLock.unlock();
        }
    }

    @Override
    public void saveWorldTag(TagWritable tag) {
        super.saveWorldTag(tag);

        if (terraformInstanceStorage != null) terraformInstanceStorage.save(tag);
        tag.setTag(SPAWN_CHECKPOINT_EFFECTS, instance().getTag(SPAWN_CHECKPOINT_EFFECTS));
    }

    //endregion World Lifecycle

    //region Test World Lifecycle

    @NonBlocking
    public <World extends AbstractMapWorld<?, ?> & SubWorld> void testWorldOrCreate(@BlockingExecutor Consumer<World> callback) {
        FutureUtil.submitVirtual(() -> {
            AbstractMapWorld<?, ?> testWorld = this.testWorld;
            testWorldLock.lock();
            try {
                if (this.testWorld == null) {
                    this.testWorld = testWorld = createTestWorld();
                    this.testWorld.loadWorld();
                }
            } finally {
                testWorldLock.unlock();
            }

            if (testWorld != null) callback.accept((World) testWorld);
        });
    }

    public @Nullable AbstractMapWorld<?, ?> testWorld() {
        return testWorld;
    }

    protected AbstractMapWorld<?, ?> createTestWorld() {
        return new TestParkourMapWorld(this);
    }

    //endregion

    // region Player Lifecycle

    @Override
    public @Nullable MapWorld canonicalWorld(Player player, Class<? extends MapWorld> type) {
        if (type.isAssignableFrom(ParkourMapWorld.class) && getPlayerState(player) instanceof EditorState.Testing)
            return testWorld;
        return super.canonicalWorld(player, type);
    }

    @Override
    public EditorState configurePlayer(Player player) {
        final var playerData = PlayerData.fromPlayer(player);
        SaveState saveState;
        try {
            saveState = server().mapService().getLatestSaveState(map().id(), playerData.id(),
                SaveStateType.EDITING, EditState.SERIALIZER);
        } catch (MapService.NotFoundError ignored) {
            // No save state yet, create one locally.
            // We do an upsert to save, so it will be created in the map service at that point.
            saveState = new SaveState(UUID.randomUUID().toString(),
                map().id(), playerData.id(), SaveStateType.EDITING,
                EditState.SERIALIZER, new EditState());
            saveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        }

        if (terraform != null) {
            terraform.initPlayerSession(player, playerData.id());
            terraform.initLocalSession(player, instance(), playerData.id());
        }

        player.setRespawnPoint(Objects.requireNonNullElseGet(
            saveState.state(EditState.class).pos(),
            () -> map().settings().getSpawnPoint()
        ));

        return new EditorState.Building(saveState);
    }

    @Override
    public void removePlayer(Player player) {
        FutureUtil.submitVirtual(() -> savePlayerState(player, true));

        super.removePlayer(player);
    }

    @Override
    public void changePlayerState(Player player, EditorState nextState, BiPredicate<Player, EditorState> predicate) {
        super.changePlayerState(player, nextState, predicate);
        spawnEntity.updateViewableRule();
    }

    private void handlePlayerTick(PlayerTickEvent event) {
        final var player = event.getPlayer();

        var minHeight = instance().getCachedDimensionType().minY() - 20;
        if (player.getPosition().y() < minHeight)
            player.teleport(map().settings().getSpawnPoint());
    }

    private void handlePlayerSavedTeleport(MapPlayerTeleportingEvent event) {
        event.player().setTag(BackCommand.LAST_LOCATION, event.player().getPosition());
    }

    @Blocking
    private void savePlayerState(Player player, boolean remove) {
        try {
            var saveState = switch (getPlayerState(player)) {
                case EditorState.Building(var ss) -> {
                    // Save building state as it is now, for testing state we don't need to
                    // save because it was saved when entering test mode.
                    var buildState = ss.state(EditState.class);
                    buildState.setPos(player.getPosition());
                    buildState.setFlying(player.isFlying());
                    var inventory = new HashMap<Integer, ItemStack>();
                    for (int i = 0; i < player.getInventory().getInnerSize(); i++) {
                        var itemStack = player.getInventory().getItemStack(i);
                        if (!itemStack.isAir()) inventory.put(i, itemStack);
                    }
                    buildState.setInventory(inventory);
                    buildState.setSelectedSlot(player.getHeldSlot());
                    yield ss;
                }
                case EditorState.Testing(var ss) -> ss;
                case null -> null;
            };
            if (saveState == null) return;

            saveState.updatePlaytime();
            saveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));

            var playerData = PlayerData.fromPlayer(player);
            var saveStateUpdate = saveState.createUpsertRequest();
            server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), saveStateUpdate);

            logger.info("Updated data for {}", player.getUuid());
        } catch (Exception e) {
            ExceptionReporter.reportException(new RuntimeException("Failed to save player state", e), player);
        }

        try {
            // Save terraform state
            if (terraform != null) {
                terraform.saveLocalSession(player, instance(), remove);
                terraform.savePlayerSession(player, remove);
            }
        } catch (Exception e) {
            ExceptionReporter.reportException(new RuntimeException("Failed to save terraform state", e), player);
        }
    }

    // endregion

    private void handleSpawnEntityInteraction(PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof SpawnMarkerEntity entity))
            return;

        final var player = event.getPlayer();
        if (map().settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage(Component.translatable("map.spawn_point.not_parkour"));
            return;
        }

        // Open checkpoint settings view
        var checkpointData = getTag(SPAWN_CHECKPOINT_EFFECTS).toMutable();
        var host = Panel.open(player, new ActionEditorView(checkpointData, Action.Type.SPAWN, "Spawn"));
        host.setTag(ActionEditorView.ACTION_LOCATION, entity.getPosition());
        host.onClose(() -> instance().setTag(SPAWN_CHECKPOINT_EFFECTS, checkpointData.toImmutable()));
    }

    @Override
    public TaskSchedule safePointTick() {
        var nextSchedule = super.safePointTick();

        var testWorld = this.testWorld;
        if (testWorld != null) testWorld.safePointTick();

        return nextSchedule;
    }

    @Override
    public void appendDebugText(TextComponent.Builder builder) {
        super.appendDebugText(builder);

        builder.appendNewline().append(Component.text("  ʜᴀѕ_ᴛᴇѕᴛ: " + (testWorld != null)));
        if (testWorld != null) {
            testWorld.appendDebugText(builder);
        }
    }
}

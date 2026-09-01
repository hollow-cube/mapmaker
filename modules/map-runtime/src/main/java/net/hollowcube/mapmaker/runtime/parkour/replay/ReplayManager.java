package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.*;
import dev.hollowcube.replay.io.SegmentedFileReplayStorage;
import dev.hollowcube.replay.io.SegmentedReplay;
import dev.hollowcube.replay.io.SegmentedReplayStorage;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.replays.ReplayClient;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.*;
import net.hollowcube.mapmaker.runtime.replay.ApiSegmentedReplayStorage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.inventory.InventoryItemChangeEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.inventory.PlayerInventory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplayManager {
    private static final Path LOCAL_REPLAY_DIR = Path.of("workspace/replay-test");
    private static final byte[] WORLD_VERSION = ReplayHeader.worldVersion(new UUID(0, 0));

    /// The hotbar slots parkour puts run items in. Nothing else is visible on a replay, so nothing
    /// else is worth recording.
    private static final int FIRST_RECORDED_SLOT = 3;
    private static final int LAST_RECORDED_SLOT = 5;

    /// The shortest reset run worth a replay, two seconds of it.
    ///
    /// Half of every recording made is of a run that ended within three seconds of starting: a
    /// hard reset, or a player who never really began. Those cost a stored object and a row each
    /// to say nothing, so a reset this fast is dropped rather than committed. Completed runs are
    /// exempt, because a finish is worth keeping at any length and the shortest maps are finished
    /// at a pace this would throw away. It only applies to a run that is over; a player who
    /// leaves after half a second may come back and make it a long one, so leaving keeps the
    /// recording as always.
    private static final int MINIMUM_RECORDED_TICKS = 40;

    /// The built-in events, with the parkour-specific ones appended after them.
    ///
    /// WARNING: As with the built-ins, these are positional and baked into every replay recorded
    /// so far. New parkour events go on the end; nothing is ever inserted or reordered.
    public static final ReplayEventRegistry REGISTRY = ReplayEvents.builder()
        .register(RunStartEvent.class, RunStartEvent.NETWORK_TYPE)
        .register(CheckpointReachedEvent.class, CheckpointReachedEvent.NETWORK_TYPE)
        .register(CheckpointResetEvent.class, CheckpointResetEvent.NETWORK_TYPE)
        .register(RunEndEvent.class, RunEndEvent.NETWORK_TYPE)
        .register(ClearGhostBlocksEvent.class, ClearGhostBlocksEvent.NETWORK_TYPE)
        .build();

    private final ParkourMapWorld world;
    private final SegmentedReplayStorage storage;
    private final PreparedRecordings recordings;
    private final Map<String, ReplaySession> sessions = new HashMap<>();
    private final Map<String, CompletableFuture<Void>> finalizations = new ConcurrentHashMap<>();
    private final EventListener<InventoryItemChangeEvent> inventoryItemChangeListener;

    private boolean closed;
    private CompletableFuture<Void> closeFuture;

    public ReplayManager(ParkourMapWorld world) {
        this(world, defaultStorage(world));
    }

    private static SegmentedReplayStorage defaultStorage(ParkourMapWorld world) {
        var replays = world.server().api().replays;
        // Dev servers run without the replay backend. Keep those recordings on disk so they stay
        // testable rather than failing every commit.
        return replays instanceof ReplayClient.Noop
            ? new SegmentedFileReplayStorage(LOCAL_REPLAY_DIR)
            : new ApiSegmentedReplayStorage(replays);
    }

    ReplayManager(ParkourMapWorld world, SegmentedReplayStorage storage) {
        this.world = world;
        this.storage = storage;
        this.recordings = new PreparedRecordings(storage, UUID.fromString(world.map().id()));

        world.eventNode(ParkourState.Playing2.class)
            .addListener(PlayerChangeHeldSlotEvent.class, this::onHeldSlotChange)
            .addListener(PlayerHandAnimationEvent.class, this::onPlayerHandAnimation);

        // This is a hella stupid event. Doesnt tell you the player and isnt an instance event so excluded.
        // We should add our own (and fix this one in Minestom)
        this.inventoryItemChangeListener = EventListener.of(InventoryItemChangeEvent.class, this::onInventoryItemChange);
        MinecraftServer.getGlobalEventHandler()
            .addListener(inventoryItemChangeListener);
    }

    /// Loads an existing committed recording before the save state is handed to the tick thread.
    @Blocking
    public void prepareRecordingSession(SaveState saveState, PlayerData playerData) {
        var priorFinalization = finalizations.get(saveState.id());
        // Only whether the prior write landed matters here. A failed one is terminal for that
        // recording either way, and waiting on it must never deny the player their join.
        if (priorFinalization != null) priorFinalization.exceptionally(_ -> null).join();

        recordings.prepare(saveState, playerData);
    }

    /// Begins or resumes local recording for an applied playing state.
    ///
    /// This must remain non-blocking because it runs during player configuration. Discovering or
    /// loading an existing replay belongs in a separate asynchronous preparation path.
    public void resumeRecordingSession(SaveState saveState, Player player) {
        if (closed) throw new IllegalStateException("Replay manager is closed");

        if (!recordings.recordable(saveState.id())) return;
        // Save states created after preparation (a hard reset, or a first join with none stored)
        // never went through it, so recording is decided for them here.
        if (!MapFeatureFlags.REPLAY_RECORDING.test(player)) return;

        var session = sessions.get(saveState.id());
        if (session == null) {
            var replay = recordings.take(saveState.id());
            var writer = storage.writer(saveState.id(), replay);
            var worldId = UUID.fromString(world.map().id());
            session = new ReplaySession(snapshot -> replay == null
                ? ReplayRecorder.create(REGISTRY, writer, worldId, WORLD_VERSION, snapshot)
                : ReplayRecorder.resume(REGISTRY, writer, replay.requirePreamble(), worldId, WORLD_VERSION, snapshot));
            sessions.put(saveState.id(), session);
            finalizations.remove(saveState.id());
        }
        session.resume();
    }

    /// Records a parkour-specific moment in whatever recording the player's run is writing to, and
    /// nothing at all if it is not being recorded.
    ///
    /// These are instants rather than state, so nothing re-emits them into a chunk snapshot: a
    /// seek reconstructs what a run looks like, not the things that happened during it.
    public void submit(Player player, ReplayEvent event) {
        var session = getActiveSession(player);
        if (session == null) return;

        session.submit(event);
    }

    /// The progress of whatever recording the player's run is writing to, or null if it is not
    /// being recorded.
    @Nullable ReplayRecorder.Stats stats(Player player) {
        var session = getActiveSession(player);
        return session == null ? null : session.stats();
    }

    /// Closes the current replay tick before safe-point state reconfiguration is applied.
    public void endTick() {
        for (var player : world.players()) {
            var session = getActiveSession(player);
            if (session == null) continue;

            // A write failure is terminal. Drop the session rather than letting it keep buffering
            // ticks that can never reach storage.
            var failure = session.failure();
            if (failure != null) {
                dropFailedSession(player, failure);
                continue;
            }

            // Recording switched off underneath a live run. The run itself is not over, so the
            // recording is stopped rather than finished and stays resumable.
            if (!MapFeatureFlags.REPLAY_RECORDING.test(player)) {
                disableSession(player);
                continue;
            }

            // Capturing while paused would allocate IDs for entities whose spawn is dropped, so
            // the whole tick is skipped and resuming re-anchors everything instead.
            if (!session.recording()) continue;

            // Movement is diffed here rather than hooked off position changes: a tick may set a
            // position several times, and a hook only ever sees the moves it was told about.
            session.captureMovement(ReplaySession.SUBJECT_ENTITY_ID, player.getPosition(), player.getVelocity());
            // Item use before state, because applying it on playback recomputes the pose, so the
            // recorded pose has to be the last word.
            session.captureItemUse(ReplaySession.SUBJECT_ENTITY_ID, player.getItemUseHand());
            session.captureState(ReplaySession.SUBJECT_ENTITY_ID, player);
            session.captureEquipment(ReplaySession.SUBJECT_ENTITY_ID, player);
            captureGhostBlocks(player, session);
            captureOwnedEntities(player, session);
            session.advance();
        }
    }

    /// Applies replay lifecycle changes for an accepted state transition.
    public CompletableFuture<Void> applyTransition(SaveState saveState, @Nullable ParkourState nextState) {
        if (nextState instanceof ParkourState.Playing2 nextPlaying
            && saveState.id().equals(nextPlaying.saveState().id())) {
            // Checkpoint/soft resets retain the same durable replay reference.
            return CompletableFuture.completedFuture(null);
        }

        if (nextState == null) {
            // The player left, but the run itself is not over. They may rejoin and resume this
            // exact recording, so stop without finalizing it.
            return endSession(saveState.id(), false, 0);
        } else if (nextState instanceof ParkourState.Playing2 || nextState instanceof ParkourState.Finished) {
            // Either the run completed, or a hard reset abandoned it in favour of a new save
            // state. Neither will ever be appended to again.
            return endSession(saveState.id(), true,
                nextState instanceof ParkourState.Finished ? 0 : MINIMUM_RECORDED_TICKS);
        } else {
            // Spectating and testing may resume this run while the world remains alive.
            var session = sessions.get(saveState.id());
            return session == null ? CompletableFuture.completedFuture(null) : session.pause();
        }
    }

    public CompletableFuture<Void> finalization(String saveStateId) {
        return finalizations.getOrDefault(saveStateId, CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<Void> close() {
        if (closeFuture != null) return closeFuture;
        closed = true;

        MinecraftServer.getGlobalEventHandler().removeListener(inventoryItemChangeListener);

        // The world is going away, but these runs are not over; leave them resumable.
        for (var saveStateId : new ArrayList<>(sessions.keySet()))
            endSession(saveStateId, false, 0);
        recordings.clear();

        closeFuture = CompletableFuture.allOf(
            finalizations.values().toArray(CompletableFuture[]::new)
        );
        return closeFuture;
    }

    private void onInventoryItemChange(InventoryItemChangeEvent event) {
        if (!(event.getInventory() instanceof PlayerInventory inventory))
            return;
        if (inventory.getViewers().isEmpty()) return;
        var player = inventory.getViewers().iterator().next();

        var slot = event.getSlot();
        if (slot < FIRST_RECORDED_SLOT || slot > LAST_RECORDED_SLOT) return;

        var session = getActiveSession(player);
        if (session == null) return;

        session.captureItem(ReplaySession.SUBJECT_ENTITY_ID, slot, event.getNewItem());
    }

    private void onHeldSlotChange(PlayerChangeHeldSlotEvent event) {
        var session = getActiveSession(event.getPlayer());
        if (session == null) return;

        session.submit(new ChangeHeldSlotEvent(ReplaySession.SUBJECT_ENTITY_ID, event.getNewSlot()));
    }

    private void onPlayerHandAnimation(PlayerHandAnimationEvent event) {
        var session = getActiveSession(event.getPlayer());
        if (session == null) return;

        session.submit(new HandAnimationEvent(ReplaySession.SUBJECT_ENTITY_ID, event.getHand()));
    }

    /// Records the spawn, movement, and removal of everything the player owns this tick.
    private void captureOwnedEntities(Player player, ReplaySession session) {
        if (!(player instanceof MapPlayer mapPlayer)) return;

        var present = new HashSet<Entity>();
        for (var owned : mapPlayer.ownedEntities()) {
            if (!(owned instanceof Entity entity)) continue;

            present.add(entity);
            var entityId = session.trackEntity(entity);
            session.captureMovement(entityId, entity.getPosition(), entity.getVelocity());
            session.captureState(entityId, entity);
            if (entity instanceof EquipmentHandler equipment)
                session.captureEquipment(entityId, equipment);
        }
        session.releaseEntitiesOutside(present);
    }

    /// Records the ghost blocks the player sees that the world does not.
    ///
    /// Diffed from the holder rather than hooked off placement, so that anything placing a ghost
    /// block is recorded whether or not it knows replays exist.
    private void captureGhostBlocks(Player player, ReplaySession session) {
        var instance = player.getInstance();
        if (instance == null) return;

        var holder = GhostBlockHolder.forPlayerOptional(player);
        session.captureBlocks(holder == null ? Map.of() : holder.save(), instance);
    }

    private @Nullable ReplaySession getActiveSession(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world != this.world) return null;
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing2(var saveState)))
            return null;
        return sessions.get(saveState.id());
    }

    private void disableSession(Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing2(var saveState))) return;

        // Nothing may start recording this save state again until it is prepared, because only
        // preparation finds the committed recording to append to; a fresh one would replace it.
        recordings.disable(saveState.id());
        endSession(saveState.id(), false, 0);
    }

    private void dropFailedSession(Player player, Throwable failure) {
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing2(var saveState))) return;

        sessions.remove(saveState.id());
        ExceptionReporter.reportException(
            new RuntimeException("replay recording failed for save state " + saveState.id(), failure),
            player
        );
    }

    private CompletableFuture<Void> endSession(String saveStateId, boolean finished, int minimumTicks) {
        var session = sessions.remove(saveStateId);
        if (session == null) {
            return finalizations.getOrDefault(saveStateId, CompletableFuture.completedFuture(null));
        }

        var finalization = finished ? session.complete(minimumTicks) : session.stop();
        finalizations.put(saveStateId, finalization);
        // Otherwise every run this world ever hosted stays pinned for its lifetime. Anything asking
        // after removal gets a completed future, which is the right answer once it has landed.
        finalization.whenComplete((_, _) -> finalizations.remove(saveStateId, finalization));
        return finalization;
    }

    /// The committed recordings a save state may resume from, and the save states that must not be
    /// recorded to at all.
    ///
    /// Unrecordable covers two distinct reasons with the same answer: the recording is already
    /// finished, or storage could not produce a usable one. Neither may be appended to, and neither
    /// is worth denying a player their join over.
    ///
    /// Recording being switched off for a player is deliberately not one of them: it is a normal
    /// outcome rather than a broken recording, so it is never reported, and it lasts only until the
    /// save state is prepared again.
    static final class PreparedRecordings {
        private final SegmentedReplayStorage storage;
        private final UUID worldId;
        private final Map<String, SegmentedReplay> prepared = new ConcurrentHashMap<>();
        private final Set<String> unrecordable = ConcurrentHashMap.newKeySet();
        private final Set<String> disabled = ConcurrentHashMap.newKeySet();

        PreparedRecordings(SegmentedReplayStorage storage, UUID worldId) {
            this.storage = storage;
            this.worldId = worldId;
        }

        /// Loads what the save state resumes from, and decides whether it may be recorded to.
        @Blocking
        void prepare(SaveState saveState, PlayerData playerData) {
            prepared.remove(saveState.id());
            unrecordable.remove(saveState.id());
            disabled.remove(saveState.id());

            // Tested before the load so a player without recording costs nothing at join.
            if (!MapFeatureFlags.REPLAY_RECORDING.test(playerData)) {
                disabled.add(saveState.id());
                return;
            }

            try {
                var replay = storage.load(saveState.id());
                if (replay == null) return;

                // A finished recording is permanently complete. Appending to it is rejected by
                // storage, and recording a second replay under the same ID would be worse, so
                // record nothing at all.
                if (replay.finished()) {
                    unrecordable.add(saveState.id());
                    return;
                }

                replay.requirePreamble().requireCompatible(worldId, WORLD_VERSION);
                prepared.put(saveState.id(), replay);
            } catch (Exception e) {
                // This runs during player configuration, where anything thrown denies the join. A
                // recording nobody can read is not worth that, so the run goes unrecorded instead.
                unrecordable.add(saveState.id());
                ExceptionReporter.reportException(new RuntimeException(
                    "replay recording unavailable for save state " + saveState.id(), e), saveState.playerId());
            }
        }

        /// Stops a save state being recorded to until it is prepared again.
        void disable(String saveStateId) {
            prepared.remove(saveStateId);
            disabled.add(saveStateId);
        }

        boolean recordable(String saveStateId) {
            return !unrecordable.contains(saveStateId) && !disabled.contains(saveStateId);
        }

        /// The recording to resume, taken so that only the first session started for a save state
        /// resumes from it.
        @Nullable SegmentedReplay take(String saveStateId) {
            return prepared.remove(saveStateId);
        }

        void clear() {
            prepared.clear();
            unrecordable.clear();
            disabled.clear();
        }
    }
}

package net.hollowcube.mapmaker.runtime.parkour.replay;

import dev.hollowcube.replay.ReplayRecorder;
import dev.hollowcube.replay.event.AbsoluteMoveEvent;
import dev.hollowcube.replay.event.DeltaMoveEvent;
import dev.hollowcube.replay.event.DestroyEntityEvent;
import dev.hollowcube.replay.event.EntityStateEvent;
import dev.hollowcube.replay.event.ItemUseEvent;
import dev.hollowcube.replay.event.ReplayEvent;
import dev.hollowcube.replay.event.ReplayTypes;
import dev.hollowcube.replay.event.SetBlockEvent;
import dev.hollowcube.replay.event.SetItemEvent;
import dev.hollowcube.replay.event.SpawnEntityEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.mapmaker.map.util.PositionUtil;
import net.hollowcube.mapmaker.runtime.parkour.replay.event.ClearGhostBlocksEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/// A replay recording session owned by the parkour runtime.
///
/// The generic replay module owns the recorder and event serialization. The host runtime owns
/// when the session exists, which events are submitted, and how it follows player/save-state
/// lifecycle.
final class ReplaySession {
    /// The recorded subject always holds the first ID; everything else is allocated after it.
    static final int SUBJECT_ENTITY_ID = 0;

    /// The main hand is already described by the inventory and the held slot, so recording it here
    /// would only say the same thing twice.
    private static final List<EquipmentSlot> RECORDED_EQUIPMENT = List.of(
        EquipmentSlot.HELMET,
        EquipmentSlot.CHESTPLATE,
        EquipmentSlot.LEGGINGS,
        EquipmentSlot.BOOTS,
        EquipmentSlot.OFF_HAND
    );

    private enum State {
        RECORDING,
        PAUSED,
        TERMINATED
    }

    private final ReplayRecorder recorder;
    private final Int2ObjectMap<Pos> lastPositions = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Vec> lastVelocities = new Int2ObjectOpenHashMap<>();
    /// Everything below is diffed against what was last recorded, so that a tick which changed
    /// nothing costs a handful of comparisons and writes nothing at all.
    private final Int2ObjectMap<EntityStateEvent> lastStates = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Map<Integer, ItemStack>> lastItems = new Int2ObjectOpenHashMap<>();
    /// The item slots this tick has touched, held until [#advance()] so that everything a tick did
    /// to an inventory arrives as one event.
    private final Int2ObjectMap<Map<Integer, ItemStack>> pendingItems = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<ItemUseEvent> lastItemUse = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<Block> lastBlocks = new Long2ObjectOpenHashMap<>();
    /// Minestom entity IDs are server-global, unstable across sessions, and share a namespace with
    /// the subject's ID, so a replay allocates its own dense IDs instead.
    ///
    /// TODO(replay): this counter restarts when a recording is resumed in a later session, so a
    ///  resumed replay re-issues IDs the earlier session already used. Playback respawns on a
    ///  repeated ID so it stays coherent, but entities the earlier session never destroyed linger
    ///  until their ID comes back around. Persisting the counter in replay metadata would fix it.
    private final Map<Entity, Integer> entityIds = new LinkedHashMap<>();

    private State state = State.RECORDING;
    private int nextEntityId = SUBJECT_ENTITY_ID + 1;
    private CompletableFuture<Void> pauseFuture;
    private CompletableFuture<Void> terminateFuture;

    /// Takes a factory rather than a recorder because the two know about each other: the recorder
    /// calls back into this session whenever a new chunk needs a snapshot. The recorder never calls
    /// back during its own construction, so handing it [#snapshot()] here is safe.
    ReplaySession(Function<Runnable, ReplayRecorder> recorder) {
        this.recorder = recorder.apply(this::snapshot);
    }

    /// Describes everything this session is tracking, so that a seek to the chunk now beginning
    /// reconstructs the world instead of inheriting whatever the previous playback position left.
    ///
    /// Dropping the last known positions is what makes the following [#captureMovement] calls emit
    /// absolute positions rather than deltas against ticks the reader may never have seen.
    private void snapshot() {
        lastPositions.clear();
        lastVelocities.clear();
        for (var entry : entityIds.entrySet())
            recorder.submit(new SpawnEntityEvent(entry.getValue(), entry.getKey().getEntityType(), entry.getKey().getPosition()));

        for (var event : lastStates.values()) recorder.submit(event);
        for (var entry : lastItems.int2ObjectEntrySet())
            if (!entry.getValue().isEmpty())
                recorder.submit(new SetItemEvent(entry.getIntKey(), Map.copyOf(entry.getValue())));
        for (var event : lastItemUse.values()) recorder.submit(event);

        // A snapshot can describe the ghost blocks that exist, but not the ones that do not, so the
        // ghost blocks a previous playback position set are dropped rather than left behind.
        recorder.submit(new ClearGhostBlocksEvent());
        for (var entry : lastBlocks.long2ObjectEntrySet())
            recorder.submit(new SetBlockEvent(PositionUtil.unpackPosition(entry.getLongKey()), entry.getValue()));
    }

    boolean recording() {
        return state == State.RECORDING;
    }

    /// Records where an entity ended up this tick, as a delta from where it was last seen or as an
    /// exact position when there is nothing to diff against.
    ///
    /// The velocity rides along for later analysis rather than for playback, so a tick that only
    /// changed velocity is still worth an event.
    ///
    /// What is remembered is the position a reader will have rebuilt rather than the one the entity
    /// really held, so that the next delta carries the rounding of this one instead of dropping it.
    /// A move too small for the wire to describe is therefore not recorded at all, and is left to
    /// accumulate until it is large enough to be worth a step.
    void captureMovement(int entityId, Pos position, Vec velocity) {
        if (state != State.RECORDING) return;

        // Submitting before bookkeeping, because the recorder may ask for a snapshot from inside
        // submit, and that snapshot drops every last known position.
        var last = lastPositions.get(entityId);
        if (last == null) {
            recorder.submit(new AbsoluteMoveEvent(entityId, position, velocity));
            lastPositions.put(entityId, position);
        } else {
            var delta = ReplayTypes.quantizeCoordinates(position.sub(last));
            var stillness = delta.isZero() && position.sameView(last)
                && velocity.equals(lastVelocities.get(entityId));
            if (!stillness) {
                recorder.submit(new DeltaMoveEvent(entityId, delta.withView(position), velocity));
                lastPositions.put(entityId, last.add(delta).withView(position));
            }
        }
        lastVelocities.put(entityId, velocity);
    }

    /// Records the pose and metadata flags an entity is in, if they are not what it was last seen
    /// in.
    void captureState(int entityId, Entity entity) {
        if (state != State.RECORDING) return;

        var meta = entity.getEntityMeta();
        var event = new EntityStateEvent(
            entityId, entity.getPose(),
            meta.isOnFire(), entity.isSneaking(),
            meta instanceof LivingEntityMeta living && living.isInRiptideSpinAttack(),
            entity.isSprinting(), meta.isSwimming(),
            entity.isInvisible(), entity.isGlowing(), meta.isFlyingWithElytra()
        );
        if (event.equals(lastStates.get(entityId))) return;

        recorder.submit(event);
        lastStates.put(entityId, event);
    }

    /// Records an inventory slot, to be written with everything else this tick touched.
    void captureItem(int entityId, int slot, ItemStack item) {
        if (state != State.RECORDING) return;
        var pending = pendingItems.get(entityId);
        if (pending == null) pendingItems.put(entityId, pending = new LinkedHashMap<>());
        pending.put(slot, item);
    }

    /// Records the armour and off hand an entity is wearing, which are inventory slots like any
    /// other.
    void captureEquipment(int entityId, EquipmentHandler equipment) {
        if (state != State.RECORDING) return;

        for (var slot : RECORDED_EQUIPMENT)
            captureItem(entityId, SetItemEvent.slotOf(slot), equipment.getEquipment(slot));
    }

    /// Writes whatever the tick did to an inventory, as one event per entity holding only the slots
    /// whose contents actually changed.
    private void flushItems() {
        for (var entry : pendingItems.int2ObjectEntrySet()) {
            var last = lastItems.get(entry.getIntKey());
            if (last == null) lastItems.put(entry.getIntKey(), last = new LinkedHashMap<>());
            var changed = new LinkedHashMap<Integer, ItemStack>();
            for (var item : entry.getValue().entrySet())
                if (!item.getValue().equals(last.get(item.getKey())))
                    changed.put(item.getKey(), item.getValue());
            if (changed.isEmpty()) continue;

            recorder.submit(new SetItemEvent(entry.getIntKey(), changed));
            last.putAll(changed);
        }
        pendingItems.clear();
    }

    /// Records which hand an entity is using an item in, or that it has stopped.
    void captureItemUse(int entityId, @Nullable PlayerHand hand) {
        if (state != State.RECORDING) return;

        var event = new ItemUseEvent(entityId, hand);
        if (event.equals(lastItemUse.get(entityId))) return;

        recorder.submit(event);
        lastItemUse.put(entityId, event);
    }

    /// Records the blocks the player sees that the world does not.
    ///
    /// Diffing whatever the holder currently contains means a feature that places a block by any
    /// route is recorded without the recorder knowing that feature exists. `world` supplies what a
    /// position goes back to once its override is gone.
    void captureBlocks(Map<Long, Block> blocks, Block.Getter world) {
        if (state != State.RECORDING) return;

        var known = lastBlocks.long2ObjectEntrySet().iterator();
        while (known.hasNext()) {
            var position = known.next().getLongKey();
            if (blocks.containsKey(position)) continue;

            var point = PositionUtil.unpackPosition(position);
            recorder.submit(new SetBlockEvent(point, world.getBlock(point, Block.Getter.Condition.TYPE)));
            known.remove();
        }

        for (var entry : blocks.entrySet()) {
            var position = (long) entry.getKey();
            if (entry.getValue().equals(lastBlocks.get(position))) continue;

            recorder.submit(new SetBlockEvent(PositionUtil.unpackPosition(position), entry.getValue()));
            lastBlocks.put(position, entry.getValue());
        }
    }

    /// The replay-local ID for an entity, recording its spawn the first time it is seen.
    int trackEntity(Entity entity) {
        var existing = entityIds.get(entity);
        if (existing != null) return existing;

        // As in captureMovement, submitting first keeps a snapshot taken from inside submit from
        // describing an entity this call is about to spawn anyway.
        var entityId = nextEntityId++;
        submit(new SpawnEntityEvent(entityId, entity.getEntityType(), entity.getPosition()));
        entityIds.put(entity, entityId);
        return entityId;
    }

    /// Records the destruction of every tracked entity that is no longer present.
    ///
    /// Detecting this by diff rather than by hooking removal means an entity that goes away by any
    /// route is still recorded as gone.
    void releaseEntitiesOutside(Set<Entity> present) {
        var entries = entityIds.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (present.contains(entry.getKey())) continue;

            submit(new DestroyEntityEvent(entry.getValue()));
            forget(entry.getValue());
            entries.remove();
        }
    }

    /// Drops everything remembered about an entity, so that a snapshot does not describe one the
    /// recording has already destroyed.
    private void forget(int entityId) {
        lastPositions.remove(entityId);
        lastVelocities.remove(entityId);
        lastStates.remove(entityId);
        lastItems.remove(entityId);
        pendingItems.remove(entityId);
        lastItemUse.remove(entityId);
    }

    void advance() {
        if (state != State.RECORDING) return;
        flushItems();
        recorder.advance();
    }

    void submit(ReplayEvent event) {
        if (state != State.RECORDING) return;
        recorder.submit(event);
    }

    ReplayRecorder.Stats stats() {
        return recorder.stats();
    }

    /// The failure that killed the underlying recording, or null while it is still healthy.
    @Nullable Throwable failure() {
        return recorder.failure();
    }

    CompletableFuture<Void> pause() {
        if (state == State.PAUSED) return pauseFuture;
        if (state == State.TERMINATED) return terminateFuture;

        state = State.PAUSED;
        pauseFuture = recorder.flush();
        return pauseFuture;
    }

    void resume() {
        if (state != State.PAUSED) return;
        state = State.RECORDING;

        // Nothing that happened while paused was recorded, so everything tracked is stale. Drop it
        // and re-describe rather than diffing across the gap; the snapshot that the pause itself
        // took describes the world as it was before the pause, and nothing else will.
        lastPositions.clear();
        lastVelocities.clear();
        lastStates.clear();
        lastItems.clear();
        pendingItems.clear();
        lastItemUse.clear();

        // Blocks are deliberately kept. The snapshot re-described exactly what is remembered here,
        // so it is still an accurate account of what a reader has, and dropping it would leave a
        // block removed during the pause set forever.
    }

    /// Stops recording but leaves the replay resumable, for when the run itself is not over.
    CompletableFuture<Void> stop() {
        return terminate(Termination.STOPPED);
    }

    /// Permanently completes the replay, or throws it away if the run was too short to have caught
    /// anything worth keeping. Storage may start compacting a completed one once this lands.
    ///
    /// Bytes that have reached storage cannot be recalled, so a recording that has already
    /// committed is kept whatever its length. That only happens to a run that was resumed or
    /// paused, which is not the kind of run this drops.
    ///
    /// @param minimumTicks the shortest run worth a replay, or 0 to keep every run
    CompletableFuture<Void> complete(int minimumTicks) {
        return terminate(recorder.tick() >= minimumTicks || recorder.committed()
            ? Termination.FINISHED
            : Termination.DISCARDED);
    }

    private CompletableFuture<Void> terminate(Termination termination) {
        if (terminateFuture != null) return terminateFuture;

        state = State.TERMINATED;
        terminateFuture = switch (termination) {
            case STOPPED -> recorder.close();
            case FINISHED -> recorder.finish();
            case DISCARDED -> recorder.discard();
        };
        return terminateFuture;
    }

    private enum Termination {
        STOPPED,
        FINISHED,
        DISCARDED
    }
}

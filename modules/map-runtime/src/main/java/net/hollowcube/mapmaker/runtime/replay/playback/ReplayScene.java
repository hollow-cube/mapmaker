package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.event.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.Viewable;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.LivingEntityMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.EquipmentHandler;
import net.minestom.server.network.packet.server.play.BlockChangePacket;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/// Applies decoded replay events to real entities in an instance, shown only to its viewers.
///
/// This is the counterpart to recording: the library decides what a replay says happened, and this
/// decides what that looks like in a world. Anything it does not recognise is ignored, so a host
/// that adds its own events can wrap this and handle only the ones it added.
///
/// Nothing a replay does is allowed to reach the instance itself, since several replays can play in
/// one world and none of them is a thing that happened there: entities are spawned unviewable and
/// shown by hand, and blocks are sent as per-viewer overrides the way ghost blocks are.
///
/// Driven from the tick thread, like everything else that touches an instance.
public final class ReplayScene implements Viewable, Consumer<ReplayEvent>, AutoCloseable {
    /// The recorded subject, spawned up front because playback needs somewhere to put it before
    /// the first event anchors it.
    public static final int SUBJECT_ENTITY_ID = 0;

    private final Instance instance;
    private final Int2ObjectMap<Entity> entities = new Int2ObjectOpenHashMap<>();
    /// Every block the replay currently overrides, so a viewer arriving late is sent the same world
    /// an existing one is looking at, and a viewer leaving can be sent the real one back.
    private final Map<Point, Block> blocks = new LinkedHashMap<>();
    private final Set<Player> viewers = new LinkedHashSet<>();
    private final Set<Player> viewersView = Collections.unmodifiableSet(viewers);

    public ReplayScene(Instance instance, Pos origin) {
        this.instance = instance;

        var subject = new PlaybackPlayerEntity();
        subject.setAutoViewable(false);
        entities.put(SUBJECT_ENTITY_ID, subject);
        subject.setInstance(instance, origin);
    }

    /// A viewer that disconnects or leaves the instance stops being sent to and must be removed by
    /// whoever added it; nothing here watches a player's lifecycle.
    @Override
    public boolean addViewer(Player player) {
        if (!viewers.add(player)) return false;
        for (var entity : entities.values()) entity.addViewer(player);
        for (var entry : blocks.entrySet()) sendBlock(player, entry.getKey(), entry.getValue());
        return true;
    }

    @Override
    public boolean removeViewer(Player player) {
        if (!viewers.remove(player)) return false;
        for (var entity : entities.values()) entity.removeViewer(player);
        clearBlocks(player);
        return true;
    }

    @Override
    public Set<? extends Player> getViewers() {
        return viewersView;
    }

    @Override
    public void accept(ReplayEvent event) {
        switch (event) {
            // The recorded velocity is deliberately not applied: playback drives position by
            // teleport with physics off, so a velocity would only fight it on the client.
            case AbsoluteMoveEvent(int entityId, Pos position, var _) -> {
                var entity = entities.get(entityId);
                if (entity == null) return;
                entity.teleport(position);
                entity.setView(position.yaw(), position.pitch());
            }
            case DeltaMoveEvent(int entityId, Pos delta, var _) -> {
                var entity = entities.get(entityId);
                if (entity == null) return;
                entity.teleport(entity.getPosition().add(delta));
                entity.setView(delta.yaw(), delta.pitch());
            }
            case SetItemEvent(int entityId, var items) -> {
                if (entities.get(entityId) instanceof PlaybackPlayerEntity entity) {
                    for (var item : items.entrySet()) entity.setItemStack(item.getKey(), item.getValue());
                } else if (entities.get(entityId) instanceof EquipmentHandler entity) {
                    // Anything that is not a player has no inventory to put a slot in, so only the
                    // slots that are worn mean anything to it.
                    for (var item : items.entrySet()) {
                        var slot = SetItemEvent.equipmentOf(item.getKey());
                        if (slot != null) entity.setEquipment(slot, item.getValue());
                    }
                }
            }
            case ChangeHeldSlotEvent(int entityId, int slot) -> {
                if (entities.get(entityId) instanceof PlaybackPlayerEntity entity)
                    entity.setHeldSlot(slot);
            }
            case HandAnimationEvent(int entityId, PlayerHand hand) -> {
                if (!(entities.get(entityId) instanceof LivingEntity entity)) return;
                if (hand == PlayerHand.MAIN) entity.swingMainHand();
                else entity.swingOffHand();
            }
            case SpawnEntityEvent(int entityId, var entityType, Pos position) -> {
                var existing = entities.remove(entityId);
                if (existing != null) existing.remove();

                var entity = new PlaybackEntity(entityType);
                entity.setAutoViewable(false);
                entities.put(entityId, entity);
                entity.setInstance(instance, position);
                for (var viewer : viewers) entity.addViewer(viewer);
            }
            case DestroyEntityEvent(int entityId) -> {
                var entity = entities.remove(entityId);
                if (entity != null) entity.remove();
            }
            case EntityStateEvent state -> {
                var entity = entities.get(state.entityId());
                if (entity == null) return;
                var meta = entity.getEntityMeta();
                meta.setOnFire(state.onFire());
                entity.setSneaking(state.sneaking());
                entity.setSprinting(state.sprinting());
                meta.setSwimming(state.swimming());
                entity.setInvisible(state.invisible());
                entity.setGlowing(state.glowing());
                meta.setFlyingWithElytra(state.flyingWithElytra());
                if (meta instanceof LivingEntityMeta living) living.setInRiptideSpinAttack(state.riptideSpinAttack());
                // Last, because everything above derives a pose of its own.
                entity.setPose(state.pose());
            }
            case ItemUseEvent(int entityId, var hand) -> {
                if (!(entities.get(entityId) instanceof LivingEntity entity)) return;
                entity.refreshActiveHand(hand != null, hand == PlayerHand.OFF, false);
            }
            case SetBlockEvent(var position, var block) -> {
                blocks.put(position, block);
                for (var viewer : viewers) sendBlock(viewer, position, block);
            }
            default -> {
                // A host event this scene does not know about.
            }
        }
    }

    @Override
    public void close() {
        for (var viewer : List.copyOf(viewers)) removeViewer(viewer);
        for (var entity : entities.values()) entity.remove();
        entities.clear();
        blocks.clear();
    }

    /// Drops every block the replay has set, putting the real world back for its viewers.
    ///
    /// Public because the events that ask for this are a host's own; nothing generic means it.
    public void restoreBlocks() {
        for (var viewer : viewers) clearBlocks(viewer);
        blocks.clear();
    }

    /// Puts the instance's own blocks back wherever the replay has overridden one, without
    /// forgetting the overrides, since a viewer can be sent them again.
    private void clearBlocks(Player viewer) {
        for (var position : blocks.keySet()) {
            if (!instance.isChunkLoaded(position)) continue;
            sendBlock(viewer, position, instance.getBlock(position, Block.Getter.Condition.TYPE));
        }
    }

    private void sendBlock(Player viewer, Point position, Block block) {
        if (viewer.getInstance() != instance) return;
        viewer.sendPacket(new BlockChangePacket(position, block));
    }
}

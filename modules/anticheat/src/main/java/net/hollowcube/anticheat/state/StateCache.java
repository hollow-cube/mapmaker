package net.hollowcube.anticheat.state;

import net.hollowcube.anticheat.protocol.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/// The connection's player-state cache: the raw frames that, replayed in arrival order, put a
/// reader's client in the state the real client was in at the snapshot.
///
/// Keys come in two kinds. **Last-wins** keys hold one frame, because a later packet fully replaces
/// an earlier one. **Accumulate** keys hold every frame since the key was created, because the
/// packet merges into what is already there and its entries cannot be split without decoding item
/// stacks. Accumulate lists are never capped — the start state is never lossy — and are dropped
/// whole when the thing they describe goes away.
///
/// Display entities (`block_display`, `item_display`, `text_display`) are not cached at all: they
/// override none of `isPickable`, `isPushable` or `canBeCollidedWith`, so they cannot affect
/// movement. [EntityTable] still tracks their positions, so that when one turns up as the vehicle
/// of an entity that *is* kept, it can be promoted with a synthesized `add_entity` carrying its
/// current position and kept from then on.
///
/// Packet ids are 776's. All state is owned by the calling thread.
public final class StateCache {

    private static final int PLAY_ADD_ENTITY = playId("add_entity");
    private static final int PLAY_COOLDOWN = playId("cooldown");
    private static final int PLAY_GAME_EVENT = playId("game_event");
    private static final int PLAY_PLAYER_INFO_REMOVE = playId("player_info_remove");
    private static final int PLAY_PLAYER_INFO_UPDATE = playId("player_info_update");
    private static final int PLAY_SET_ENTITY_DATA = playId("set_entity_data");
    private static final int PLAY_SET_PLAYER_TEAM = playId("set_player_team");

    /// The plan's singleton-state row: one frame per packet id, last one wins.
    private static final Set<Integer> PLAY_SINGLETONS = Set.of(
        playId("initialize_border"),
        playId("player_abilities"),
        playId("set_border_center"),
        playId("set_border_lerp_size"),
        playId("set_border_size"),
        playId("set_camera"),
        playId("set_chunk_cache_center"),
        playId("set_chunk_cache_radius"),
        playId("set_cursor_item"),
        playId("set_health"),
        playId("set_held_slot"),
        playId("set_simulation_distance"),
        playId("set_time"),
        playId("ticking_state"),
        playId("update_tags"));

    /// The configuration-phase set, replaced wholesale by the next configuration phase.
    private static final Set<Integer> CONFIGURATION_SET = Set.of(
        configurationId("registry_data"),
        configurationId("update_enabled_features"),
        configurationId("update_tags"));

    private final EntityTable entities = new EntityTable();

    private Map<StateKey, StateFrame> lastWins = new HashMap<>();
    private Map<StateKey, FrameNode> accumulated = new HashMap<>();
    private boolean shared;
    private long sequence;
    private String level = "";
    private @Nullable String brand;

    public EntityTable entities() {
        return entities;
    }

    public @Nullable String brand() {
        return brand;
    }

    public int keyCount() {
        return lastWins.size() + accumulated.size();
    }

    public @Nullable StateFrame frame(StateKey key) {
        return lastWins.get(key);
    }

    public List<StateFrame> frames(StateKey key) {
        var head = accumulated.get(key);
        return head == null ? List.of() : head.toList();
    }

    /// An immutable view of the cache right now. Constant time: the view keeps this cache's maps
    /// and the next write copies them.
    public StateCacheView snapshot() {
        shared = true;
        return new StateCacheView(Collections.unmodifiableMap(lastWins),
            Collections.unmodifiableMap(accumulated), entities.snapshot(), brand);
    }

    /// Feeds one kept frame to the cache. `packet` is the decoded form when the registry has a
    /// decoder for the id, and null otherwise — the keys of undecoded packets come from a prefix of
    /// `body`, which is the payload without its id varint.
    public void apply(ProtocolState state, Direction direction, int packetId, byte[] body, @Nullable Packet packet) {
        switch (direction) {
            case C2S -> applyServerbound(packet);
            case S2C -> {
                switch (state) {
                    case CONFIGURATION -> applyConfiguration(packetId, body);
                    case PLAY -> applyPlay(packetId, body, packet);
                    case HANDSHAKE, LOGIN -> {
                    }
                }
            }
        }
    }

    private void applyServerbound(@Nullable Packet packet) {
        switch (packet) {
            case MovePlayer move -> entities.apply(move);
            case CustomPayload payload -> {
                var payloadBrand = payload.brand();
                if (payloadBrand != null) brand = payloadBrand;
            }
            // The rest of the C2S stream is recorded, but none of it is state a reader has to be
            // put into before the frames start.
            case null, default -> {
            }
        }
    }

    private void applyConfiguration(int packetId, byte[] body) {
        if (CONFIGURATION_SET.contains(packetId))
            append(StateKey.Config.INSTANCE, frame(ProtocolState.CONFIGURATION, packetId, body));
    }

    private void applyPlay(int packetId, byte[] body, @Nullable Packet packet) {
        switch (packet) {
            // Boundaries first: what is kept afterwards depends on the state they leave behind.
            case S2CStartConfiguration _ -> resetAll();
            case S2CLogin login -> applyLogin(login, packetId, body);
            case S2CRespawn respawn -> applyRespawn(respawn, packetId, body);

            // Position-only packets feed the entity table and are never cached: replaying a
            // relative move on its own would be wrong, and the prelude reads positions out of the
            // table instead.
            case MoveEntity move -> entities.apply(move);
            case S2CTeleportEntity teleport -> entities.apply(teleport);
            case S2CEntityPositionSync sync -> entities.apply(sync);
            case S2CPlayerPosition position -> entities.apply(position);
            case S2CPlayerRotation rotation -> entities.apply(rotation);

            case S2CAddEntity add -> {
                entities.apply(add);
                putEntity(add.entityId(), packetId, body);
            }
            case S2CRemoveEntities remove -> {
                forgetEntities(remove.entityIds());
                entities.apply(remove);
            }
            case S2CUpdateMobEffect effect -> {
                if (!entities.isDropped(effect.entityId()))
                    put(new StateKey.Effect(effect.entityId(), effect.effectId()),
                        frame(ProtocolState.PLAY, packetId, body));
            }
            case S2CRemoveMobEffect effect -> remove(new StateKey.Effect(effect.entityId(), effect.effectId()));
            case S2CSetPassengers passengers -> applyPassengers(passengers, packetId, body);
            case S2CSetEntityData data -> {
                entities.apply(data);
                appendEntity(data.entityId(), packetId, body);
            }
            case S2CSetEquipment equipment -> appendEntity(equipment.entityId(), packetId, body);
            case S2CUpdateAttributes attributes -> {
                if (entities.isDropped(attributes.entityId())) break;
                for (var snapshot : attributes.attributes()) {
                    put(new StateKey.EntityAttribute(attributes.entityId(), snapshot.attribute()), frame(ProtocolState.PLAY, packetId,
                        new S2CUpdateAttributes.V776(attributes.entityId(), List.of(snapshot)).toByteArray()));
                }
            }
            case S2CSetEntityLink link -> putEntity(link.entityId(), packetId, body);
            case S2CContainerSetContent content -> {
                removeKeys(key -> key instanceof StateKey.ContainerSlot slot
                    && slot.containerId() == content.containerId());
                put(new StateKey.Container(content.containerId(), packetId), frame(ProtocolState.PLAY, packetId, body));
            }
            case S2CContainerSetSlot slot -> put(new StateKey.ContainerSlot(slot.containerId(), slotOf(slot)),
                frame(ProtocolState.PLAY, packetId, body));
            case S2CSetPlayerInventory inventory -> put(new StateKey.InventorySlot(inventory.slot()),
                frame(ProtocolState.PLAY, packetId, body));

            // A packet with no key of its own, decoded or not, is keyed by its id and whatever
            // prefix of the payload splits it further.
            case null, default -> applyById(packetId, body);
        }
    }

    /// `handleLogin` builds a new level and a new player, so the play phase starts over around it.
    private void applyLogin(S2CLogin packet, int packetId, byte[] body) {
        entities.apply(packet);
        resetPlay();
        level = packet.spawnInfo().dimension();
        put(new StateKey.Login(packetId), frame(ProtocolState.PLAY, packetId, body));
    }

    /// The packets that are kept but never decoded, whose key is the id plus whatever prefix of the
    /// payload tells two of them apart.
    private void applyById(int packetId, byte[] body) {
        if (packetId == PLAY_GAME_EVENT) {
            if (body.length > 0)
                put(new StateKey.Singleton(packetId, body[0] & 0xFF), frame(ProtocolState.PLAY, packetId, body));
        } else if (packetId == PLAY_COOLDOWN) {
            var group = leadingString(body);
            if (group != null) put(new StateKey.Cooldown(group), frame(ProtocolState.PLAY, packetId, body));
        } else if (packetId == PLAY_SET_PLAYER_TEAM) {
            applyTeam(packetId, body);
        } else if (packetId == PLAY_PLAYER_INFO_UPDATE) {
            applyPlayerInfo(packetId, body);
        } else if (packetId == PLAY_PLAYER_INFO_REMOVE) {
            applyPlayerInfoRemove(packetId, body);
        } else if (PLAY_SINGLETONS.contains(packetId)) {
            put(new StateKey.Singleton(packetId), frame(ProtocolState.PLAY, packetId, body));
        }
    }

    /// Nothing about a dropped display entity is cached, so its per-entity keys never come into
    /// existence in the first place.
    private void putEntity(int entityId, int packetId, byte[] body) {
        if (entities.isDropped(entityId)) return;
        put(new StateKey.Entity(entityId, packetId), frame(ProtocolState.PLAY, packetId, body));
    }

    private void appendEntity(int entityId, int packetId, byte[] body) {
        if (entities.isDropped(entityId)) return;
        append(new StateKey.Entity(entityId, packetId), frame(ProtocolState.PLAY, packetId, body));
    }

    /// `handleRespawn` only rebuilds the level when the dimension changes, so entities and their
    /// effects only go then; the open container is closed either way. The player is rebuilt with
    /// them, but the new one is given the old one's attribute bases — every value with
    /// `KEEP_ATTRIBUTE_MODIFIERS`, its metadata with `KEEP_ENTITY_DATA` — so those keys stay: a
    /// base the server set once, in an earlier level, is what the client still moves by.
    private void applyRespawn(S2CRespawn packet, int packetId, byte[] body) {
        if (!packet.spawnInfo().dimension().equals(level)) {
            int playerId = entities.player().entityId();
            boolean keepModifiers = (packet.dataToKeep() & S2CRespawn.KEEP_ATTRIBUTE_MODIFIERS) != 0;
            boolean keepEntityData = (packet.dataToKeep() & S2CRespawn.KEEP_ENTITY_DATA) != 0;
            entities.clear();
            removeKeys(key -> switch (key) {
                case StateKey.Entity entity -> entity.entityId() != playerId || entity.packetId() != PLAY_SET_ENTITY_DATA || !keepEntityData;
                case StateKey.EntityAttribute attribute -> attribute.entityId() != playerId;
                case StateKey.Effect _ -> true;
                default -> false;
            });
            if (!keepModifiers) stripModifiers(playerId);
        }
        removeKeys(key -> key instanceof StateKey.Container || key instanceof StateKey.ContainerSlot);
        level = packet.spawnInfo().dimension();
        put(new StateKey.Login(packetId), frame(ProtocolState.PLAY, packetId, body));
    }

    /// `AttributeMap#assignBaseValues`: the bases without the modifiers.
    private void stripModifiers(int playerId) {
        for (var entry : List.copyOf(lastWins.entrySet())) {
            if (!(entry.getKey() instanceof StateKey.EntityAttribute attribute) || attribute.entityId() != playerId) continue;
            var snapshot = S2CUpdateAttributes.V776.decode(new ByteReader(entry.getValue().body())).attributes().getFirst();
            if (snapshot.modifiers().isEmpty()) continue;
            var stripped = new S2CUpdateAttributes.V776(playerId,
                List.of(new S2CUpdateAttributes.Snapshot(snapshot.attribute(), snapshot.base(), List.of())));
            put(attribute, frame(ProtocolState.PLAY, entry.getValue().packetId(), stripped.toByteArray()));
        }
    }

    /// The promotion guard: a dropped display entity that is the vehicle of anything still kept
    /// (including the local player and any id we never saw an `add_entity` for) comes back, with a
    /// synthesized `add_entity` built from its tracked position.
    private void applyPassengers(S2CSetPassengers packet, int packetId, byte[] body) {
        int vehicle = packet.entityId();
        if (entities.isDropped(vehicle) && carriesKeptEntity(packet)) promote(vehicle);
        if (entities.isDropped(vehicle)) return;
        put(new StateKey.Entity(vehicle, packetId), frame(ProtocolState.PLAY, packetId, body));
    }

    private boolean carriesKeptEntity(S2CSetPassengers packet) {
        for (int passenger : packet.passengerIds())
            if (!entities.isDropped(passenger)) return true;
        return false;
    }

    private void promote(int entityId) {
        var entity = entities.promote(entityId);
        if (entity == null) return;
        var synthesized = new S2CAddEntity.V776(
            entity.entityId(),
            Objects.requireNonNullElse(entity.uuid(), new UUID(0, entity.entityId())),
            entity.typeId(),
            entity.x(), entity.y(), entity.z(),
            LpVec3.ZERO,
            TrackedEntity.packRotation(entity.xRot()),
            TrackedEntity.packRotation(entity.yRot()),
            TrackedEntity.packRotation(entity.yRot()),
            0);
        put(new StateKey.Entity(entityId, PLAY_ADD_ENTITY),
            frame(ProtocolState.PLAY, PLAY_ADD_ENTITY, synthesized.toByteArray()));
    }

    private void applyTeam(int packetId, byte[] body) {
        try {
            var reader = new ByteReader(body);
            var name = reader.utf();
            int method = reader.i8();
            // ClientboundSetPlayerTeamPacket: 0 add, 1 remove, 2 update, 3/4 membership.
            if (method == 1) remove(new StateKey.Team(name));
            else put(new StateKey.Team(name), frame(ProtocolState.PLAY, packetId, body));
        } catch (ProtocolException _) {
            // A team packet we cannot key is a team packet we cannot replay in the right order.
        }
    }

    private void applyPlayerInfo(int packetId, byte[] body) {
        var entries = PlayerInfoEntries.split(body);
        if (entries == null) {
            append(new StateKey.PlayerInfo(null), frame(ProtocolState.PLAY, packetId, body));
            return;
        }
        for (var entry : entries) {
            var key = new StateKey.PlayerInfo(entry.uuid());
            if (entry.added()) remove(key);
            append(key, frame(ProtocolState.PLAY, packetId, entry.body()));
        }
    }

    private void applyPlayerInfoRemove(int packetId, byte[] body) {
        var removed = PlayerInfoEntries.removed(body);
        if (removed == null) return;
        for (UUID uuid : removed) remove(new StateKey.PlayerInfo(uuid));
        // The un-splittable bucket has to see the removal too, since its adds are still in there.
        if (accumulated.containsKey(new StateKey.PlayerInfo(null)))
            append(new StateKey.PlayerInfo(null), frame(ProtocolState.PLAY, packetId, body));
    }

    private void forgetEntities(int[] entityIds) {
        var removed = new HashSet<Integer>(entityIds.length);
        for (int entityId : entityIds) removed.add(entityId);
        removeKeys(key -> switch (key) {
            case StateKey.Entity entity -> removed.contains(entity.entityId());
            case StateKey.EntityAttribute attribute -> removed.contains(attribute.entityId());
            case StateKey.Effect effect -> removed.contains(effect.entityId());
            default -> false;
        });
    }

    /// `handleLogin` builds a new level, a new player and a fresh packet listener, so everything the
    /// play phase put in the cache goes; the configuration set carries over into it.
    private void resetPlay() {
        removeKeys(key -> !(key instanceof StateKey.Config));
        level = "";
    }

    private void resetAll() {
        lastWins = new HashMap<>();
        accumulated = new HashMap<>();
        shared = false;
        entities.clear();
        level = "";
    }

    private StateFrame frame(ProtocolState state, int packetId, byte[] body) {
        return new StateFrame(sequence++, state, Direction.S2C, packetId, body);
    }

    private void put(StateKey key, StateFrame frame) {
        lastWins().put(key, frame);
    }

    private void append(StateKey key, StateFrame frame) {
        var accumulated = accumulated();
        accumulated.put(key, FrameNode.append(accumulated.get(key), frame));
    }

    private void remove(StateKey key) {
        if (lastWins.containsKey(key)) lastWins().remove(key);
        if (accumulated.containsKey(key)) accumulated().remove(key);
    }

    private void removeKeys(Predicate<StateKey> matches) {
        var doomed = new ArrayList<StateKey>();
        for (StateKey key : lastWins.keySet()) if (matches.test(key)) doomed.add(key);
        if (!doomed.isEmpty()) {
            var lastWins = lastWins();
            for (StateKey key : doomed) lastWins.remove(key);
            doomed.clear();
        }
        for (StateKey key : accumulated.keySet()) if (matches.test(key)) doomed.add(key);
        if (doomed.isEmpty()) return;
        var accumulated = accumulated();
        for (StateKey key : doomed) accumulated.remove(key);
    }

    private Map<StateKey, StateFrame> lastWins() {
        unshare();
        return lastWins;
    }

    private Map<StateKey, FrameNode> accumulated() {
        unshare();
        return accumulated;
    }

    private void unshare() {
        if (!shared) return;
        lastWins = new HashMap<>(lastWins);
        accumulated = new HashMap<>(accumulated);
        shared = false;
    }

    /// `ClientboundContainerSetSlotPacket` is `containerId, varint stateId, short slot, item`, and
    /// the record keeps everything after the container id as bytes.
    private static int slotOf(S2CContainerSetSlot packet) {
        try {
            var reader = new ByteReader(packet.rest());
            reader.varInt();
            return reader.i16();
        } catch (ProtocolException _) {
            return -1;
        }
    }

    private static @Nullable String leadingString(byte[] body) {
        try {
            return new ByteReader(body).utf();
        } catch (ProtocolException _) {
            return null;
        }
    }

    private static int playId(String name) {
        return Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, name);
    }

    private static int configurationId(String name) {
        return Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.S2C, name);
    }
}

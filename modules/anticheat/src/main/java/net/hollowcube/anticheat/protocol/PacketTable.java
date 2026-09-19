package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// One protocol version's packet ids and what the capture does with each of them, per state and
/// direction. The versions themselves are [Protocol776] and its successors, each listing its
/// packets in the registration order of `GameProtocols` and `ConfigurationProtocols` —
/// `ProtocolInfoBuilder` hands out ids as the index into the list it builds, so position in a
/// table *is* the packet id.
public final class PacketTable {

    @FunctionalInterface
    public interface Decoder {
        Packet decode(ByteReader reader);
    }

    /// A fence the table cannot decide on the id alone: asked of the decoded packet, once the
    /// engine has it, and a yes means a ping is due exactly as if the entry were in the ping set.
    @FunctionalInterface
    public interface PingWhen {
        boolean fence(Packet packet, int localPlayerId);
    }

    /// What the tap does with a frame.
    ///
    /// [#PING_SET] is [#KEEP] plus "a ping is injected after this frame". Decoded packets can be
    /// in the ping set too, so [Entry#pingSet()] — not the kind — is the query the tap makes;
    /// the kind only ever says what to do with the payload.
    public enum KeepKind {
        /// Not recorded, passed through untouched.
        DROP,
        /// Recorded as raw bytes.
        KEEP,
        /// Recorded as raw bytes and decoded into a [Packet] for the world model and trim.
        KEEP_DECODE,
        /// Recorded as raw bytes, and a state change the reader has to be able to time.
        PING_SET
    }

    public record Entry(String name, KeepKind kind, boolean pingSet, @Nullable Decoder decoder,
                        @Nullable PingWhen pingWhen) {

        public boolean kept() {
            return kind != KeepKind.DROP;
        }
    }

    /// The answer for an id the version does not define. Unknown ids are passed through, never
    /// recorded: a frame we cannot name is a frame we cannot replay.
    public static final Entry UNKNOWN = new Entry("unknown", KeepKind.DROP, false, null, null);

    /// Fence when the packet lands on the local player: what is per-entity noise for everyone else
    /// is knockback, a speed change or a hitbox change when the entity is the player themself.
    static final PingWhen SELF =
        (packet, localPlayerId) -> packet instanceof EntityKeyed keyed && keyed.entityId() == localPlayerId;

    /// `stopSleeping` writes the bed block, teleports the entity and changes its pose — the only
    /// animate action that mutates the world, for any entity.
    static final PingWhen WAKE_UP = (packet, _) -> packet instanceof S2CAnimate animate && animate.wakesUp();

    /// Events 9 and 55 on the local player carry state no other packet does: the end of item use
    /// (the sprint gate) and the hand swap. Everything else in the packet is cosmetic.
    static final PingWhen SELF_EVENT = (packet, localPlayerId) ->
        packet instanceof S2CEntityEvent event && event.entityId() == localPlayerId
            && (event.event() == S2CEntityEvent.USE_ITEM_COMPLETE || event.event() == S2CEntityEvent.SWAP_HANDS);

    private final Table handshakeC2S;
    private final Table loginC2S;
    private final Table loginS2C;
    private final Table configurationC2S;
    private final Table configurationS2C;
    private final Table playC2S;
    private final Table playS2C;

    PacketTable(Builder handshakeC2S, Builder loginC2S, Builder loginS2C, Builder configurationC2S,
                Builder configurationS2C, Builder playC2S, Builder playS2C) {
        this.handshakeC2S = handshakeC2S.build();
        this.loginC2S = loginC2S.build();
        this.loginS2C = loginS2C.build();
        this.configurationC2S = configurationC2S.build();
        this.configurationS2C = configurationS2C.build();
        this.playC2S = playC2S.build();
        this.playS2C = playS2C.build();
    }

    public List<Entry> entries(ProtocolState state, Direction direction) {
        return select(state, direction).entries;
    }

    public Entry lookup(ProtocolState state, Direction direction, int packetId) {
        var entries = select(state, direction).entries;
        return packetId >= 0 && packetId < entries.size() ? entries.get(packetId) : UNKNOWN;
    }

    /// Lookup by the vanilla packet type name (`GamePacketTypes` and friends, without the
    /// `minecraft:` namespace), so tests can name packets the way the audit does.
    public Entry lookup(ProtocolState state, Direction direction, String name) {
        var table = select(state, direction);
        var id = table.ids.get(name);
        return id == null ? UNKNOWN : table.entries.get(id);
    }

    /// The packet id of a named packet, or -1 when the version does not have it.
    public int packetId(ProtocolState state, Direction direction, String name) {
        var id = select(state, direction).ids.get(name);
        return id == null ? -1 : id;
    }

    private Table select(ProtocolState state, Direction direction) {
        return switch (state) {
            case HANDSHAKE -> switch (direction) {
                case C2S -> handshakeC2S;
                case S2C -> Table.EMPTY;
            };
            case LOGIN -> switch (direction) {
                case C2S -> loginC2S;
                case S2C -> loginS2C;
            };
            case CONFIGURATION -> switch (direction) {
                case C2S -> configurationC2S;
                case S2C -> configurationS2C;
            };
            case PLAY -> switch (direction) {
                case C2S -> playC2S;
                case S2C -> playS2C;
            };
        };
    }

    private record Table(List<Entry> entries, Map<String, Integer> ids) {
        static final Table EMPTY = new Table(List.of(), Map.of());
    }

    static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        Builder drop(String name) {
            return add(new Entry(name, KeepKind.DROP, false, null, null));
        }

        Builder keep(String name) {
            return add(new Entry(name, KeepKind.KEEP, false, null, null));
        }

        Builder ping(String name) {
            return add(new Entry(name, KeepKind.PING_SET, true, null, null));
        }

        Builder decode(String name, Decoder decoder) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, false, decoder, null));
        }

        Builder decodePing(String name, Decoder decoder) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, true, decoder, null));
        }

        Builder decodePingWhen(String name, Decoder decoder, PingWhen when) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, false, decoder, when));
        }

        private Builder add(Entry entry) {
            entries.add(entry);
            return this;
        }

        private Table build() {
            var ids = new HashMap<String, Integer>(entries.size());
            for (int id = 0; id < entries.size(); id++) ids.put(entries.get(id).name(), id);
            return new Table(List.copyOf(entries), Map.copyOf(ids));
        }
    }
}

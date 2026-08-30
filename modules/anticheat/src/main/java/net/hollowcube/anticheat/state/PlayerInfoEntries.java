package net.hollowcube.anticheat.state;

import net.hollowcube.anticheat.protocol.ByteReader;
import net.hollowcube.anticheat.protocol.ByteWriter;
import net.hollowcube.anticheat.protocol.ProtocolException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Splits a `player_info_update` into one single-entry packet per profile uuid, which is what lets
/// the cache key those packets by uuid without a full decoder for the entry payloads.
///
/// The layout is `EnumSet<Action>` as an eight-bit fixed bitset (there are exactly eight actions in
/// 26.2), a varint entry count, then per entry a uuid followed by the payload of each set action in
/// declaration order. Splitting only needs each payload's *length*, and every one of them is
/// reachable with the reader's primitives.
///
/// The split is self-checking: if any payload length is wrong the reader either overruns or leaves
/// bytes behind, and [#split] answers null so the caller can keep the frame whole instead.
final class PlayerInfoEntries {

    static final int ADD_PLAYER = 1;
    private static final int INITIALIZE_CHAT = 1 << 1;
    private static final int UPDATE_GAME_MODE = 1 << 2;
    private static final int UPDATE_LISTED = 1 << 3;
    private static final int UPDATE_LATENCY = 1 << 4;
    private static final int UPDATE_DISPLAY_NAME = 1 << 5;
    private static final int UPDATE_LIST_ORDER = 1 << 6;
    private static final int UPDATE_HAT = 1 << 7;

    record Entry(UUID uuid, boolean added, byte[] body) {}

    static @Nullable List<Entry> split(byte[] body) {
        try {
            var reader = new ByteReader(body);
            int actions = reader.u8();
            int count = reader.varInt();
            if (count < 0 || count > reader.remaining()) return null;

            var entries = new ArrayList<Entry>(count);
            for (int i = 0; i < count; i++) {
                int start = reader.index();
                var uuid = reader.uuid();
                skipEntry(reader, actions);
                var writer = new ByteWriter();
                writer.u8(actions).varInt(1).bytes(reader.since(start));
                entries.add(new Entry(uuid, (actions & ADD_PLAYER) != 0, writer.toByteArray()));
            }
            return reader.remaining() == 0 ? List.copyOf(entries) : null;
        } catch (ProtocolException _) {
            return null;
        }
    }

    /// The uuids of a `player_info_remove`, or null when it does not parse.
    static @Nullable List<UUID> removed(byte[] body) {
        try {
            var reader = new ByteReader(body);
            int count = reader.varInt();
            if (count < 0 || count * 16 > reader.remaining()) return null;
            var uuids = new ArrayList<UUID>(count);
            for (int i = 0; i < count; i++) uuids.add(reader.uuid());
            return reader.remaining() == 0 ? List.copyOf(uuids) : null;
        } catch (ProtocolException _) {
            return null;
        }
    }

    private static void skipEntry(ByteReader reader, int actions) {
        if ((actions & ADD_PLAYER) != 0) {
            reader.utf(16); // ByteBufCodecs.PLAYER_NAME
            int properties = reader.varInt();
            if (properties < 0 || properties > reader.remaining()) throw new ProtocolException("bad property count");
            for (int i = 0; i < properties; i++) {
                reader.utf(64);
                reader.utf();
                if (reader.bool()) reader.utf(1024);
            }
        }
        if ((actions & INITIALIZE_CHAT) != 0 && reader.bool()) {
            reader.uuid(); // session id
            reader.i64(); // ProfilePublicKey.Data expiry instant
            reader.byteArray(); // encoded public key
            reader.byteArray(); // key signature
        }
        if ((actions & UPDATE_GAME_MODE) != 0) reader.varInt();
        if ((actions & UPDATE_LISTED) != 0) reader.bool();
        if ((actions & UPDATE_LATENCY) != 0) reader.varInt();
        if ((actions & UPDATE_DISPLAY_NAME) != 0 && reader.bool()) reader.skipNbt();
        if ((actions & UPDATE_LIST_ORDER) != 0) reader.varInt();
        if ((actions & UPDATE_HAT) != 0) reader.bool();
    }

    private PlayerInfoEntries() {}
}

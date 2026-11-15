package net.hollowcube.mapmaker.map.wal;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.network.NetworkBuffer;

import java.util.ArrayList;
import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

public record WalBatch(long firstSeq, List<WalRecord> records) {
    private static final int VERSION = 1;

    private static final int COMPRESSION_NONE = 0;
    private static final int COMPRESSION_ZSTD = 1;
    private static final int ZSTD_LEVEL = 3;

    private static final int TYPE_EDIT = 1;
    private static final int TYPE_WORLD_TAG = 2;

    public WalBatch {
        if (records.isEmpty()) throw new IllegalArgumentException("a batch holds at least one record");
        records = List.copyOf(records);
    }

    public long lastSeq() {
        return firstSeq + records.size() - 1;
    }

    // Writes one batch, safe to write multiple in a row (they mark their own end)
    public byte[] encode(NetworkBuffer.Type<EditBuffer> edits) {
        long baseTime = records.getFirst().time();
        byte[] raw = NetworkBuffer.makeArray(buffer -> {
            long previous = baseTime;
            for (WalRecord record : records) {
                buffer.write(BYTE, (byte) switch (record) {
                    case WalRecord.Edit _ -> TYPE_EDIT;
                    case WalRecord.WorldTag _ -> TYPE_WORLD_TAG;
                });
                buffer.write(VAR_LONG, Math.max(0, record.time() - previous));
                previous = Math.max(previous, record.time());
                buffer.write(BYTE_ARRAY, NetworkBuffer.makeArray(payload -> {
                    switch (record) {
                        case WalRecord.Edit edit -> payload.write(edits, edit.buffer());
                        case WalRecord.WorldTag tag -> {
                            payload.write(VAR_INT, tag.dataVersion());
                            payload.write(STRING, tag.key());
                            payload.write(BOOLEAN, tag.value() != null);
                            if (tag.value() != null) payload.write(NBT, tag.value());
                        }
                    }
                }));
            }
        });

        byte[] compressed;
        try (var zstd = new ZstdCompressCtx()) {
            compressed = zstd.setLevel(ZSTD_LEVEL).setChecksum(true).compress(raw);
        }
        boolean compress = compressed.length < raw.length;
        byte[] body = compress ? compressed : raw;
        return NetworkBuffer.makeArray(buffer -> {
            buffer.write(VAR_INT, VERSION);
            buffer.write(VAR_INT, compress ? COMPRESSION_ZSTD : COMPRESSION_NONE);
            buffer.write(VAR_LONG, firstSeq);
            buffer.write(VAR_INT, records.size());
            buffer.write(LONG, baseTime);
            buffer.write(VAR_INT, raw.length);
            buffer.write(BYTE_ARRAY, body);
        });
    }

    /// Reads one batch, leaving `buffer` at the start of the next.
    public static WalBatch decode(NetworkBuffer buffer, NetworkBuffer.Type<EditBuffer> edits) {
        int version = buffer.read(VAR_INT);
        if (version != VERSION) throw new IllegalStateException("unknown wal batch version " + version);
        int compression = buffer.read(VAR_INT);
        long firstSeq = buffer.read(VAR_LONG);
        int count = buffer.read(VAR_INT);
        long time = buffer.read(LONG);
        int rawLength = buffer.read(VAR_INT);
        byte[] body = buffer.read(BYTE_ARRAY);
        byte[] raw = switch (compression) {
            case COMPRESSION_NONE -> body;
            case COMPRESSION_ZSTD -> Zstd.decompress(body, rawLength);
            default -> throw new IllegalStateException("unknown wal batch compression " + compression);
        };
        if (raw.length != rawLength)
            throw new IllegalStateException("wal batch body is " + raw.length + " bytes, expected " + rawLength);

        var in = NetworkBuffer.wrap(raw, 0, raw.length);
        List<WalRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int type = in.read(BYTE);
            time += in.read(VAR_LONG);
            byte[] payload = in.read(BYTE_ARRAY);
            var data = NetworkBuffer.wrap(payload, 0, payload.length);
            switch (type) {
                case TYPE_EDIT -> records.add(new WalRecord.Edit(time, data.read(edits)));
                case TYPE_WORLD_TAG -> {
                    int dataVersion = data.read(VAR_INT);
                    String key = data.read(STRING);
                    records.add(new WalRecord.WorldTag(time, dataVersion, key, data.read(BOOLEAN) ? data.read(NBT) : null));
                }
                default -> throw new IllegalStateException("unknown wal record type " + type);
            }
        }
        return new WalBatch(firstSeq, records);
    }
}

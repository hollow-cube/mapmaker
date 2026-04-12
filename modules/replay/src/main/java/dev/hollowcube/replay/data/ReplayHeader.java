package dev.hollowcube.replay.data;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;

import java.util.UUID;

public final class ReplayHeader {
    private static final int HEADER_LENGTH = 256;
    public static final int MAGIC = 0x48435250; // 'HCRP'

    public static final short VERSION_LATEST = 1;

    public static final short FLAGS_NONE = 0; // unused for now

    private final short version; // format version
    private final UUID worldId;
    private final UUID worldVersion;
    private final long timestamp;
    private final short dictionary;

    private int metadataLength = 0;
    private int indexLength = 0;
    private int tickCount = 0;
    private int chunkCount = 0;

    /// Create a new replay header
    public ReplayHeader(UUID worldId, UUID worldVersion) {
        this.version = VERSION_LATEST;
        this.worldId = worldId;
        this.worldVersion = worldVersion;
        this.timestamp = System.currentTimeMillis();
        this.dictionary = VERSION_LATEST;
    }

    /// Read a previously saved header back
    public ReplayHeader(NetworkBuffer buffer) {
        long startIndex = buffer.readIndex();

        int magic = buffer.read(NetworkBuffer.INT);
        Check.argCondition(magic != MAGIC, "corrupt header");

        this.version = buffer.read(NetworkBuffer.SHORT);
        Check.argCondition(version > VERSION_LATEST, "unsupported replay version: {0}", version);

        buffer.read(NetworkBuffer.SHORT); // flags
        this.worldId = buffer.read(NetworkBuffer.UUID);
        this.worldVersion = buffer.read(NetworkBuffer.UUID);
        this.timestamp = buffer.read(NetworkBuffer.LONG);
        this.dictionary = buffer.read(NetworkBuffer.SHORT);
        this.metadataLength = buffer.read(NetworkBuffer.INT);
        this.indexLength = buffer.read(NetworkBuffer.INT);
        this.tickCount = buffer.read(NetworkBuffer.INT);
        this.chunkCount = buffer.read(NetworkBuffer.INT);

        // Skip the remaining fixed header length
        buffer.advanceRead(HEADER_LENGTH - (buffer.readIndex() - startIndex));
    }

    public void update(int metadataLength, int indexLength, int tickCount, int chunkCount) {
        this.metadataLength = metadataLength;
        this.indexLength = indexLength;
        this.tickCount = tickCount;
        this.chunkCount = chunkCount;
    }

    public void write(NetworkBuffer buffer) {
        long startIndex = buffer.writeIndex();

        buffer.write(NetworkBuffer.INT, MAGIC);
        buffer.write(NetworkBuffer.SHORT, version);
        buffer.write(NetworkBuffer.SHORT, FLAGS_NONE);
        buffer.write(NetworkBuffer.UUID, worldId);
        buffer.write(NetworkBuffer.UUID, worldVersion);
        buffer.write(NetworkBuffer.LONG, timestamp);
        buffer.write(NetworkBuffer.SHORT, dictionary);
        buffer.write(NetworkBuffer.INT, metadataLength);
        buffer.write(NetworkBuffer.INT, indexLength);
        buffer.write(NetworkBuffer.INT, tickCount);
        buffer.write(NetworkBuffer.INT, chunkCount);

        // Pad to the remaining fixed header length
        buffer.advanceWrite(HEADER_LENGTH - (buffer.writeIndex() - startIndex));
    }
}

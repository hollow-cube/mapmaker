package net.hollowcube.anticheat.protocol;

import com.github.luben.zstd.ZstdInputStream;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Reads the `.hcpt.zst` captures the client-side packet tap writes.
///
/// Whole-file zstd; inside, `"HCPT"`, `u16 version`, `u32 header length`, a JSON header, then
/// frames of `i64 tNs, u8 direction, u8 state, i32 packetId, i32 length, bytes`. Everything is
/// big-endian, `direction` is 0 for client to server, `state` is the vanilla `ConnectionProtocol`
/// ordinal, and the frame bytes still carry the packet id varint the length prefix covers.
public record FixtureReader(Header header, List<Frame> frames) {

    public static final int MAGIC = 0x48435054; // 'HCPT'
    public static final int VERSION = 1;

    public record Header(int pv, String brand, String mode, String mcVersion) {}

    public record Frame(long tNs, Direction direction, ProtocolState state, int packetId, byte[] body) {}

    public static FixtureReader read(Path file) {
        byte[] bytes;
        try (InputStream in = new ZstdInputStream(Files.newInputStream(file))) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var reader = new ByteReader(bytes);
        if (reader.i32() != MAGIC) throw new ProtocolException("not a fixture file: " + file);
        int version = reader.u16();
        if (version != VERSION) throw new ProtocolException("unsupported fixture version: " + version);
        var header = new Gson().fromJson(new String(reader.bytes(reader.i32()), StandardCharsets.UTF_8), Header.class);

        var frames = new ArrayList<Frame>();
        while (reader.remaining() > 0) {
            long tNs = reader.i64();
            var direction = reader.u8() == 0 ? Direction.C2S : Direction.S2C;
            var state = state(reader.u8());
            int packetId = reader.i32();
            var frame = reader.slice(reader.i32());
            if (frame.varInt() != packetId) throw new ProtocolException("frame id does not match its varint prefix");
            frames.add(new Frame(tNs, direction, state, packetId, frame.remainingBytes()));
        }
        return new FixtureReader(header, List.copyOf(frames));
    }

    /// The vanilla `ConnectionProtocol` ordinals. Status never appears in a gameplay capture.
    private static ProtocolState state(int id) {
        return switch (id) {
            case 0 -> ProtocolState.HANDSHAKE;
            case 1 -> ProtocolState.PLAY;
            case 3 -> ProtocolState.LOGIN;
            case 4 -> ProtocolState.CONFIGURATION;
            default -> throw new ProtocolException("unexpected protocol state: " + id);
        };
    }
}

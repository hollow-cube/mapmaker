package net.hollowcube.anticheat.log;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/// One kept packet, exactly as the client parsed it.
///
/// [#bytes] is the packet body **after** its id varint, which is what the per-version decoders
/// expect and what keeps the id from being stored twice. [#tNs] is relative to the trace start,
/// not a wall clock: the trace's job is ordering and spacing, and [TraceHeader#startedAt] is
/// where absolute time lives.
///
/// Layout: `varLong tDeltaNs, u8 (state << 1 | direction), varInt packetId, varInt pingId,
/// varInt length, body`. Time is stored as the delta to the previous frame in the stream —
/// nondecreasing arrival order makes it small — so [#encode]/[#decode] carry the previous frame's
/// [#tNs] rather than standing alone. `pingId` is the tap's sequential counter (see
/// [Frame#NO_PING]); the `0x8000_0000` bit the injected ping *packet* carries is wire encoding,
/// not part of the id.
public record Frame(
    long tNs,
    Direction direction,
    ProtocolState state,
    int packetId,
    int pingId,
    byte[] bytes
) {

    /// No ping had been injected yet when this frame crossed the tap; real ids count up from 1.
    public static final int NO_PING = 0;

    public Frame {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(bytes, "bytes");
    }

    public void encode(DataOutput out, long previousTNs) throws IOException {
        TraceFormat.writeVarLong(out, tNs - previousTNs);
        out.writeByte(TraceFormat.stateCode(state) << 1 | TraceFormat.directionCode(direction));
        TraceFormat.writeVarInt(out, packetId);
        TraceFormat.writeVarInt(out, pingId);
        TraceFormat.writeBytes(out, bytes);
    }

    public static Frame decode(DataInput in, long previousTNs) throws IOException {
        long tNs = previousTNs + TraceFormat.readVarLong(in);
        int packed = in.readUnsignedByte();
        var state = TraceFormat.state(packed >> 1);
        var direction = TraceFormat.direction(packed & 1);
        int packetId = TraceFormat.readVarInt(in);
        int pingId = TraceFormat.readVarInt(in);
        return new Frame(tNs, direction, state, packetId, pingId, TraceFormat.readBytes(in));
    }
}

package net.hollowcube.anticheat.state;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;

/// One frame held by the [StateCache], stored as the raw bytes it arrived as so the prelude can
/// replay it verbatim.
///
/// [#body] is the packet payload without its leading id varint, the same slice the per-version
/// decoders read. [#sequence] is the order the cache saw the frame in, which is the order the
/// prelude has to emit them in — a synthesized frame gets the sequence it was synthesized at.
public record StateFrame(long sequence, ProtocolState state, Direction direction, int packetId, byte[] body) {
}

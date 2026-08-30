package net.hollowcube.proxy.anticheat;

import net.hollowcube.anticheat.capture.FrameSink;
import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;

import java.util.ArrayList;
import java.util.List;

/// Everything the tap kept, in order. The engine is tested against these fixtures in
/// `modules/anticheat`; here the question is only what the tap hands it.
final class RecordingSink implements FrameSink {

    record Kept(long tNs, Direction direction, ProtocolState state, int packetId, int pingId, byte[] body) {
    }

    final List<Kept> frames = new ArrayList<>();
    boolean disconnected;
    boolean shutdown;
    boolean closed;
    /// Returned from every [#frame] call, standing in for the engine's conditional fences.
    boolean fence;

    @Override
    public boolean frame(long tNs, Direction direction, ProtocolState state, int packetId, int pingId, byte[] body) {
        frames.add(new Kept(tNs, direction, state, packetId, pingId, body));
        return fence;
    }

    @Override
    public void disconnect(boolean shutdown) {
        disconnected = true;
        this.shutdown = shutdown;
    }

    @Override
    public void close() {
        closed = true;
    }
}

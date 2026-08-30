package net.hollowcube.anticheat.capture;

import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.ProtocolState;

/// Where the tap hands the frames it keeps, and the only thing the Netty side of the capture needs
/// to know about the engine.
///
/// Every method is called on the connection's event loop, one connection to one sink, so an
/// implementation owns its state outright and never locks.
public interface FrameSink extends AutoCloseable {

    /// One kept frame. `tNs` is [CaptureClock#nanoTime()] at the tap, `pingId` is the id of the
    /// last ping injected before this frame (or [net.hollowcube.anticheat.log.Frame#NO_PING]), and
    /// `body` is the packet payload without its id varint, which this sink takes ownership of.
    ///
    /// True when the frame asks for a ping fence the packet table could not decide on the id
    /// alone — the sink is the side that decodes, so conditions like "lands on the local player"
    /// are answered here and the tap treats a yes exactly like a ping-set frame.
    boolean frame(long tNs, Direction direction, ProtocolState state, int packetId, int pingId, byte[] body);

    /// The connection went away. Anything open is closed out as far as it got, and `shutdown`
    /// says whether the proxy is going down with it rather than the player leaving of their own
    /// accord — velocity shuts down by disconnecting everybody first, so the two look identical
    /// from the channel.
    void disconnect(boolean shutdown);

    /// Releases everything, waiting a bounded time for work already handed off.
    @Override
    void close();
}

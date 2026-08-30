package net.hollowcube.proxy.anticheat;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.prometheus.client.Counter;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.anticheat.capture.FrameSink;
import net.hollowcube.anticheat.log.Frame;
import net.hollowcube.anticheat.protocol.Direction;
import net.hollowcube.anticheat.protocol.Protocol776;
import net.hollowcube.anticheat.protocol.ProtocolState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BooleanSupplier;

/// The netty half of the capture: it copies the frames a connection carries out of the pipeline,
/// tracks the protocol state those frames are read in, and injects the pings that let a reader
/// bracket when the client saw a change.
///
/// **Two pipeline entries, not one.** Velocity's player pipeline is
/// `cipher-decoder → frame-decoder → compression-decoder → [via-decoder] → minecraft-decoder →
/// compression-encoder → [via-encoder] → minecraft-encoder → handler`, and outbound traversal runs
/// the other way, so *no single position* sees plain frames both ways: before `minecraft-decoder`
/// the inbound bytes are decompressed but the outbound ones have already been through
/// `compression-encoder`. Via solves this with a handler on each side and so do we —
/// [#DECODER_NAME] goes before `via-decoder`/`minecraft-decoder` and [#ENCODER_NAME] before
/// `via-encoder`/`minecraft-encoder`. Both then see uncompressed frames in the *client's* protocol:
/// C2S before via rewrites it, S2C after via rewrote it.
///
/// Every field is owned by the channel's event loop, which both entries share, so nothing here
/// locks. Frames are copied into `byte[]` and handed straight to the sink; the tap decodes nothing
/// beyond the leading packet id (and the four bytes of a pong id).
///
/// Nothing the tap does may cost a player their connection: a failure in here disables the tap for
/// that connection and every frame passes through untouched from then on.
public final class AnticheatTap {

    private static final Logger logger = LoggerFactory.getLogger(AnticheatTap.class);

    /// Inbound entry, before `via-decoder` when via is installed.
    public static final String DECODER_NAME = "hc-anticheat";
    /// Outbound entry, before `via-encoder` when via is installed.
    public static final String ENCODER_NAME = "hc-anticheat-encoder";

    /// Pipeline handler names from ViaVersion 5.12.0 (ViaDecodeHandler, ViaEncodeHandler,
    /// VelocityChannelInitializer).
    static final String VIA_DECODER = "via-decoder";
    static final String VIA_ENCODER = "via-encoder";
    static final String MINECRAFT_DECODER = "minecraft-decoder";
    static final String MINECRAFT_ENCODER = "minecraft-encoder";

    /// The wire encoding of an injected ping id, which keeps it clear of the backend's space: the
    /// backend counts up from zero (`MapPlayer#ping`), the tap sends its sequence with this bit
    /// set. The bit is wire-only — frames and headers carry the bare sequence, counted from 1.
    public static final int PROXY_PING_BIT = 0x8000_0000;

    /// How many injected ping ids stay answerable at once. A client answers a ping within a tick or
    /// two and almost always in order, so this only has to cover reordering, and bounding it is
    /// what stops an unanswered ping from leaking a slot for the life of the connection.
    private static final int OUTSTANDING_PINGS = 64;

    private static final int LOGIN_FINISHED = id(ProtocolState.LOGIN, Direction.S2C, "login_finished");
    private static final int LOGIN_ACKNOWLEDGED = id(ProtocolState.LOGIN, Direction.C2S, "login_acknowledged");
    private static final int CONFIGURATION_FINISH_S2C = id(ProtocolState.CONFIGURATION, Direction.S2C, "finish_configuration");
    private static final int CONFIGURATION_FINISH_C2S = id(ProtocolState.CONFIGURATION, Direction.C2S, "finish_configuration");
    private static final int CONFIGURATION_PONG = id(ProtocolState.CONFIGURATION, Direction.C2S, "pong");
    private static final int PLAY_START_CONFIGURATION = id(ProtocolState.PLAY, Direction.S2C, "start_configuration");
    private static final int PLAY_CONFIGURATION_ACKNOWLEDGED = id(ProtocolState.PLAY, Direction.C2S, "configuration_acknowledged");
    private static final int PLAY_BUNDLE_DELIMITER = id(ProtocolState.PLAY, Direction.S2C, "bundle_delimiter");
    private static final int PLAY_PING = id(ProtocolState.PLAY, Direction.S2C, "ping");
    private static final int PLAY_PONG = id(ProtocolState.PLAY, Direction.C2S, "pong");

    private final FrameSink sink;
    private final CaptureClock clock;
    private final BooleanSupplier shuttingDown;

    /// Per-direction counter children resolved once per connection: `labels()` allocates on every
    /// call and [#keep] runs per frame. Instance rather than static so a test that `clear()`s the
    /// counters and then builds a tap still sees its increments.
    private final Counter.Child c2sFrames = AnticheatMetrics.frames.labels(AnticheatMetrics.Dir.C2S.label);
    private final Counter.Child s2cFrames = AnticheatMetrics.frames.labels(AnticheatMetrics.Dir.S2C.label);
    private final Counter.Child c2sBytes = AnticheatMetrics.bytes.labels(AnticheatMetrics.Dir.C2S.label);
    private final Counter.Child s2cBytes = AnticheatMetrics.bytes.labels(AnticheatMetrics.Dir.S2C.label);

    private final Decoder decoder = new Decoder();
    private final Encoder encoder = new Encoder();

    private ProtocolState c2sState;
    private ProtocolState s2cState;

    /// The last ping the tap injected, which every frame after it is stamped with so a reader knows
    /// which bracket the frame fell in.
    private int lastPingId = Frame.NO_PING;
    private int pingCounter;
    private boolean pingSetSinceLastPing;
    /// Ids this tap injected and has not seen answered, oldest overwritten first; 0 is free, and no
    /// injected id is ever 0 because they all carry [#PROXY_PING_BIT].
    private final int[] outstandingPings = new int[OUTSTANDING_PINGS];
    private int outstandingCursor;
    /// Depth is not needed: bundles do not nest, the delimiter opens and closes one.
    private boolean inBundle;

    private boolean failed;
    private boolean disconnected;
    private long exceptions;

    /// The states each direction is in when the tap goes in. They are tracked separately because
    /// the protocol switches them separately: the server moves its outbound state on
    /// `login_finished`/`finish_configuration`/`start_configuration` and its inbound state only
    /// when the client acknowledges, and frames sent in between are read in the old table.
    ///
    /// `shuttingDown` is asked once, when the channel goes inactive, for the one thing the channel
    /// cannot say for itself: whether the proxy is going down and took the player with it.
    public AnticheatTap(FrameSink sink, CaptureClock clock, BooleanSupplier shuttingDown,
                        ProtocolState c2sState, ProtocolState s2cState) {
        this.sink = sink;
        this.clock = clock;
        this.shuttingDown = shuttingDown;
        this.c2sState = c2sState;
        this.s2cState = s2cState;
    }

    /// Puts both entries in `pipeline`, ahead of via when via is there. False when the pipeline is
    /// not one we recognise, in which case nothing was added.
    public boolean install(ChannelPipeline pipeline) {
        var beforeDecoder = pipeline.get(VIA_DECODER) != null ? VIA_DECODER : MINECRAFT_DECODER;
        var beforeEncoder = pipeline.get(VIA_ENCODER) != null ? VIA_ENCODER : MINECRAFT_ENCODER;
        if (pipeline.get(beforeDecoder) == null || pipeline.get(beforeEncoder) == null) {
            logger.warn("anticheat: not tapping, no {}/{} in the pipeline: {}",
                beforeDecoder, beforeEncoder, pipeline.names());
            return false;
        }
        pipeline.addBefore(beforeDecoder, DECODER_NAME, decoder);
        pipeline.addBefore(beforeEncoder, ENCODER_NAME, encoder);
        logger.debug("anticheat: tapped before {}/{}", beforeDecoder, beforeEncoder);
        return true;
    }

    public ProtocolState c2sState() {
        return c2sState;
    }

    public ProtocolState s2cState() {
        return s2cState;
    }

    /// The last ping id injected, or [Frame#NO_PING] before the first one.
    public int lastPingId() {
        return lastPingId;
    }

    /// Exceptions the pipeline handed the tap, which are passed on rather than swallowed.
    public long exceptions() {
        return exceptions;
    }

    /// True once the tap gave up on this connection; frames still pass through untouched.
    public boolean failed() {
        return failed;
    }

    //region C2S

    private void read(ChannelHandlerContext context, ByteBuf buffer) {
        if (failed) {
            context.fireChannelRead(buffer);
            return;
        }
        boolean swallowed;
        try {
            swallowed = readFrame(buffer);
        } catch (RuntimeException | LinkageError e) {
            fail(e);
            context.fireChannelRead(buffer);
            return;
        }
        if (swallowed) {
            buffer.release();
            return;
        }
        context.fireChannelRead(buffer);
    }

    /// Records the frame and says whether it answered a ping *this* tap injected and is still
    /// waiting on, which the backend must never see: it counts ping ids up from zero and would take
    /// ours for one of its own.
    ///
    /// Being in the proxy's id space is not enough on its own. A pong carries whatever id it is
    /// answering, and the client will happily send ids in that space that this tap never issued: a
    /// connection replayed from a capture taken behind another tap carries the first one's pongs,
    /// and a client that pongs the same id twice only had one ping to answer. Anything the tap
    /// cannot account for goes on to the backend, which is where an id it did not issue came from.
    private boolean readFrame(ByteBuf buffer) {
        long peeked = peekId(buffer);
        if (peeked < 0) return false;
        int packetId = (int) peeked;
        int idLength = (int) (peeked >>> 32);
        var state = c2sState;

        boolean ours = isPong(state, packetId)
            && buffer.readableBytes() >= idLength + Integer.BYTES
            && answerOutstanding(buffer.getInt(buffer.readerIndex() + idLength));

        keep(Direction.C2S, state, packetId, buffer, idLength);
        if (ours) {
            AnticheatMetrics.pongsSwallowed.inc();
            return true;
        }
        advanceC2S(state, packetId);
        return false;
    }

    //endregion

    //region S2C

    private void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
        if (!failed && message instanceof ByteBuf buffer) {
            try {
                writeFrame(buffer);
            } catch (RuntimeException | LinkageError e) {
                fail(e);
            }
        }
        context.write(message, promise);
    }

    private void writeFrame(ByteBuf buffer) {
        long peeked = peekId(buffer);
        if (peeked < 0) return;
        int packetId = (int) peeked;
        var state = s2cState;

        if (keep(Direction.S2C, state, packetId, buffer, (int) (peeked >>> 32))) pingSetSinceLastPing = true;
        if (state == ProtocolState.PLAY && packetId == PLAY_BUNDLE_DELIMITER) inBundle = !inBundle;
        advanceS2C(state, packetId);
    }

    /// Injects a ping when something the client's state depends on was written since the last one,
    /// so a reader can bracket the change between the ping and its pong. Never inside a bundle: the
    /// client holds a bundle's packets until the delimiter closes, which would move the ping to
    /// after the very frames it is timing.
    private void flush(ChannelHandlerContext context) {
        if (failed || !pingSetSinceLastPing || inBundle || s2cState != ProtocolState.PLAY) {
            context.flush();
            return;
        }
        ByteBuf ping = null;
        try {
            int pingId = ++pingCounter;
            int wireId = PROXY_PING_BIT | pingId;
            ping = context.alloc().buffer(varIntLength(PLAY_PING) + Integer.BYTES);
            writeVarInt(ping, PLAY_PING);
            ping.writeInt(wireId);

            lastPingId = pingId;
            pingSetSinceLastPing = false;
            outstandingPings[outstandingCursor] = wireId;
            outstandingCursor = (outstandingCursor + 1) % outstandingPings.length;
            AnticheatMetrics.pings.inc();
            // The ping is a frame the client saw, and the first one of its own bracket.
            keep(Direction.S2C, ProtocolState.PLAY, PLAY_PING, ping, varIntLength(PLAY_PING));

            var written = ping;
            ping = null;
            context.write(written);
        } catch (RuntimeException | LinkageError e) {
            fail(e);
        } finally {
            if (ping != null) ping.release();
        }
        context.flush();
    }

    //endregion

    //region Shared

    /// Hands a kept frame to the sink and counts it, answering whether a ping fence is due after
    /// it — either the table says so on the id alone, or the sink did once it had the decoded
    /// packet. The body is copied out of the buffer, which is left exactly as it was found: the
    /// frame still has to pass through untouched.
    private boolean keep(Direction direction, ProtocolState state, int packetId,
                         ByteBuf buffer, int idLength) {
        if (!tracked(state)) {
            AnticheatMetrics.dropped(AnticheatMetrics.Drop.UNKNOWN_STATE);
            return false;
        }
        var entry = Protocol776.lookup(state, direction, packetId);
        if (!entry.kept()) return false;

        var body = new byte[buffer.readableBytes() - idLength];
        buffer.getBytes(buffer.readerIndex() + idLength, body);
        boolean fence = sink.frame(clock.nanoTime(), direction, state, packetId, lastPingId, body);

        (direction == Direction.C2S ? c2sFrames : s2cFrames).inc();
        (direction == Direction.C2S ? c2sBytes : s2cBytes).inc(idLength + (double) body.length);
        return entry.pingSet() || fence;
    }

    /// True when `pingId` is an injected ping still waiting to be answered, and takes it off the
    /// list on the way out so the next pong carrying that id is somebody else's.
    private boolean answerOutstanding(int pingId) {
        for (int i = 0; i < outstandingPings.length; i++) {
            if (outstandingPings[i] != pingId) continue;
            outstandingPings[i] = 0;
            return true;
        }
        return false;
    }

    /// The states the tap has a packet table and a transition out of. Handshake is not one of
    /// them — the intention packet is long past by the time the tap goes in and nothing moves a
    /// direction back into it — so a frame read there is one the tap cannot name a table for, and
    /// a frame it cannot name is one it cannot replay: it passes through, counted rather than kept.
    private static boolean tracked(ProtocolState state) {
        return switch (state) {
            case LOGIN, CONFIGURATION, PLAY -> true;
            case HANDSHAKE -> false;
        };
    }

    private void advanceC2S(ProtocolState state, int packetId) {
        switch (state) {
            case LOGIN -> {
                if (packetId == LOGIN_ACKNOWLEDGED) c2sState = ProtocolState.CONFIGURATION;
            }
            case CONFIGURATION -> {
                if (packetId == CONFIGURATION_FINISH_C2S) c2sState = ProtocolState.PLAY;
            }
            case PLAY -> {
                if (packetId == PLAY_CONFIGURATION_ACKNOWLEDGED) c2sState = ProtocolState.CONFIGURATION;
            }
            case HANDSHAKE -> {
                // The tap goes in at PostLoginEvent, long after the intention packet.
            }
        }
    }

    private void advanceS2C(ProtocolState state, int packetId) {
        switch (state) {
            case LOGIN -> {
                if (packetId == LOGIN_FINISHED) s2cState = ProtocolState.CONFIGURATION;
            }
            case CONFIGURATION -> {
                if (packetId == CONFIGURATION_FINISH_S2C) enterPlay();
            }
            case PLAY -> {
                if (packetId == PLAY_START_CONFIGURATION) leavePlay();
            }
            case HANDSHAKE -> {
            }
        }
    }

    private void enterPlay() {
        s2cState = ProtocolState.PLAY;
        inBundle = false;
    }

    /// Configuration has no ping set at all (nothing in its table changes state the reader has to
    /// time), so anything pending from the play phase is dropped rather than carried across.
    private void leavePlay() {
        s2cState = ProtocolState.CONFIGURATION;
        inBundle = false;
        pingSetSinceLastPing = false;
    }

    private void disconnect() {
        if (disconnected) return;
        disconnected = true;
        try {
            sink.disconnect(shuttingDown.getAsBoolean());
        } catch (RuntimeException e) {
            logger.warn("anticheat: capture failed to close out", e);
        }
    }

    private void fail(Throwable cause) {
        failed = true;
        logger.warn("anticheat: tap disabled for this connection", cause);
    }

    private static boolean isPong(ProtocolState state, int packetId) {
        return switch (state) {
            case PLAY -> packetId == PLAY_PONG;
            case CONFIGURATION -> packetId == CONFIGURATION_PONG;
            case HANDSHAKE, LOGIN -> false;
        };
    }

    private static int id(ProtocolState state, Direction direction, String name) {
        int id = Protocol776.packetId(state, direction, name);
        if (id < 0) throw new IllegalStateException("no 776 packet named " + name + " in " + state + " " + direction);
        return id;
    }

    /// The packet id at the reader index and how long it was, packed as `length << 32 | id`, or -1
    /// when the frame does not start with a whole varint. Reads nothing: the buffer is passed on.
    private static long peekId(ByteBuf buffer) {
        int from = buffer.readerIndex();
        int limit = buffer.writerIndex();
        int value = 0;
        for (int i = 0; i < 5; i++) {
            if (from + i >= limit) return -1;
            int part = buffer.getByte(from + i) & 0xFF;
            value |= (part & 0x7F) << (i * 7);
            if ((part & 0x80) == 0) return ((long) (i + 1) << 32) | Integer.toUnsignedLong(value);
        }
        return -1;
    }

    private static void writeVarInt(ByteBuf buffer, int value) {
        while ((value & ~0x7F) != 0) {
            buffer.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        buffer.writeByte(value);
    }

    private static int varIntLength(int value) {
        int length = 1;
        while ((value & ~0x7F) != 0) {
            value >>>= 7;
            length++;
        }
        return length;
    }

    //endregion

    /// Before `via-decoder`, so the C2S bytes are the ones the client wrote.
    private final class Decoder extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext context, Object message) {
            if (message instanceof ByteBuf buffer) read(context, buffer);
            else context.fireChannelRead(message);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            disconnect();
            super.channelInactive(context);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext context) {
            disconnect();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            exceptions++;
            context.fireExceptionCaught(cause);
        }
    }

    /// Before `via-encoder`, so the S2C bytes are the ones the client will parse, and after
    /// `minecraft-encoder`, so they are still uncompressed.
    private final class Encoder extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
            AnticheatTap.this.write(context, message, promise);
        }

        @Override
        public void flush(ChannelHandlerContext context) {
            AnticheatTap.this.flush(context);
        }
    }
}

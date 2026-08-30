package net.hollowcube.proxy.anticheat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import net.hollowcube.anticheat.capture.CaptureClock;
import net.hollowcube.anticheat.protocol.ByteWriter;

import java.time.Instant;

/// A stand-in for velocity's player pipeline, in the order the real one ends up in once compression
/// is on: `frame-decoder → compression-decoder → [via-decoder] → minecraft-decoder →
/// compression-encoder → [via-encoder] → minecraft-encoder`.
///
/// The order is what the tap's placement is about, so the placeholders are laid out exactly like
/// the real thing rather than as a flat list of names: inbound runs head to tail and outbound runs
/// tail to head, which is the whole reason the tap needs an entry on each side.
final class TapPipeline {

    static EmbeddedChannel channel(AnticheatTap tap, boolean via) {
        var channel = new EmbeddedChannel();
        var pipeline = channel.pipeline();
        pipeline.addLast("frame-decoder", new ChannelDuplexHandler());
        pipeline.addLast("compression-decoder", new ChannelDuplexHandler());
        if (via) pipeline.addLast(AnticheatTap.VIA_DECODER, new ChannelDuplexHandler());
        pipeline.addLast(AnticheatTap.MINECRAFT_DECODER, new ChannelDuplexHandler());
        pipeline.addLast("compression-encoder", new ChannelDuplexHandler());
        if (via) pipeline.addLast(AnticheatTap.VIA_ENCODER, new ChannelDuplexHandler());
        pipeline.addLast(AnticheatTap.MINECRAFT_ENCODER, new ChannelDuplexHandler());
        if (tap != null) tap.install(pipeline);
        return channel;
    }

    /// One frame as it sits on the wire between the framing and the codec: the id varint then the
    /// body.
    static byte[] frame(int packetId, byte[] body) {
        return new ByteWriter(body.length + 5).varInt(packetId).bytes(body).toByteArray();
    }

    static ByteBuf buffer(byte[] bytes) {
        return Unpooled.wrappedBuffer(bytes);
    }

    static byte[] bytes(ByteBuf buf) {
        var copy = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), copy);
        return copy;
    }

    /// Time the test owns outright, so frames land at the fixture's own spacing.
    static final class ManualClock implements CaptureClock {

        private long nanos;

        @Override
        public long nanoTime() {
            return nanos;
        }

        @Override
        public Instant instant() {
            return Instant.EPOCH.plusNanos(nanos);
        }

        void set(long nanos) {
            this.nanos = nanos;
        }
    }

    private TapPipeline() {
    }
}

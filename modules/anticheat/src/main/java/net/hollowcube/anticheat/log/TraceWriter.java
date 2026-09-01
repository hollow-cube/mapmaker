package net.hollowcube.anticheat.log;

import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/// Writes a trace, streaming: the header region and the parts that come from the start snapshot
/// (prelude frames, world chunks) are written when the writer opens, and every frame after that is
/// appended as it arrives.
///
/// The header is rewritten in place at [#close()] with the counters this writer kept, which is what
/// makes the streaming shape survive a crash — see [TraceFormat] for why the header goes first.
/// Only the fields learned at close may change; a final header that no longer fits its reserved
/// region is a programming error, not a runtime condition.
public final class TraceWriter implements AutoCloseable {

    /// The zstd stream is flushed at least this often, so a crash costs at most this much of the
    /// tail. Frames are individually tiny and flushing each one would cost a zstd block apiece.
    public static final int FLUSH_INTERVAL_BYTES = 1 << 20;

    private final FileChannel channel;
    private final ZstdOutputStreamNoFinalizer compressor;
    private final DataOutputStream body;
    private final int headerCapacity;
    private final long preludeFrames;
    private final int chunks;

    private TraceHeader header;
    private long frames;
    private long frameBytes;
    private long sinceFlush;
    /// The previous frame's time, prelude included: frame times are stored as deltas, so writer
    /// and reader walk the same chain.
    private long lastTNs;
    private boolean closed;

    private TraceWriter(FileChannel channel, TraceHeader header, List<Frame> prelude, TraceWorld world) throws IOException {
        this.channel = channel;
        // Stamped here rather than by the caller: which dictionary a body is compressed against is
        // this writer's business, and the header has to name it before the stream starts.
        this.header = header.withDictionary(TraceDictionary.LATEST);

        var json = this.header.toJson().getBytes(StandardCharsets.UTF_8);
        this.headerCapacity = json.length + TraceFormat.HEADER_SLACK;
        writeHead(json);

        var out = new BufferedOutputStream(new KeepOpen(Channels.newOutputStream(channel)), 1 << 16);
        this.compressor = new ZstdOutputStreamNoFinalizer(out, TraceFormat.COMPRESSION_LEVEL);
        var dictionary = TraceDictionary.compress(this.header.dictionaryId());
        if (dictionary != null) this.compressor.setDict(dictionary);
        this.body = new DataOutputStream(this.compressor);

        TraceFormat.writeVarInt(this.body, prelude.size());
        for (var frame : prelude) {
            frame.encode(this.body, lastTNs);
            lastTNs = frame.tNs();
        }
        this.preludeFrames = prelude.size();

        var chunks = world.chunks();
        TraceFormat.writeVarInt(this.body, chunks.size());
        for (var chunk : chunks) chunk.encode(this.body);
        this.chunks = chunks.size();
    }

    public static TraceWriter open(Path path, TraceHeader header, List<Frame> prelude, TraceWorld world) {
        if (header.formatVersion() != TraceFormat.VERSION_LATEST) {
            throw new IllegalArgumentException(
                "trace header is version " + header.formatVersion() + ", this writer only writes "
                    + TraceFormat.VERSION_LATEST);
        }
        FileChannel channel = null;
        try {
            channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            return new TraceWriter(channel, header, prelude, world);
        } catch (IOException e) {
            closeQuietly(channel);
            throw new UncheckedIOException("failed to open trace " + path, e);
        }
    }

    /// Writes the whole trace in one call, for the assembly the capture engine does at stop: the
    /// start snapshot's prelude and world, then every spooled frame.
    ///
    /// Returns the header as it landed on disk, counters filled in.
    public static TraceHeader assemble(
        Path path,
        TraceHeader header,
        List<Frame> prelude,
        TraceWorld world,
        FrameSource frames
    ) {
        try (var writer = open(path, header, prelude, world)) {
            frames.forEach(writer::frame);
            return writer.finish();
        }
    }

    public void frame(Frame frame) {
        try {
            frame.encode(body, lastTNs);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write trace frame", e);
        }
        lastTNs = frame.tNs();
        frames++;
        frameBytes += frame.bytes().length;

        sinceFlush += frame.bytes().length;
        if (sinceFlush >= FLUSH_INTERVAL_BYTES) flush();
    }

    /// Makes everything written so far readable by a reader that never sees the rest of the file.
    public void flush() {
        try {
            body.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("failed to flush trace", e);
        }
        sinceFlush = 0;
    }

    /// Replaces the pending header, for the fields only known once the capture is over.
    public void header(TraceHeader header) {
        this.header = header;
    }

    /// Closes the body and rewrites the header with the counters, returning what landed on disk.
    public TraceHeader finish() {
        header = header.withCounters(new TraceHeader.Counters(
            frames, frameBytes, preludeFrames, chunks, header.counters().droppedFrames()));
        close();
        return header;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try (var channel = this.channel) {
            body.close();
            var json = header.toJson().getBytes(StandardCharsets.UTF_8);
            if (json.length > headerCapacity) {
                throw new IllegalStateException("trace header grew past its reserved region: "
                    + json.length + " > " + headerCapacity);
            }
            channel.write(ByteBuffer.wrap(json), TraceFormat.FIXED_HEAD_LENGTH);
            var length = ByteBuffer.allocate(4).putInt(json.length).flip();
            channel.write(length, TraceFormat.FIXED_HEAD_LENGTH - 4);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to close trace", e);
        }
    }

    private void writeHead(byte[] json) throws IOException {
        var head = ByteBuffer.allocate(TraceFormat.FIXED_HEAD_LENGTH + headerCapacity);
        head.putInt(TraceFormat.MAGIC);
        head.putShort((short) TraceFormat.VERSION_LATEST);
        head.putInt(headerCapacity);
        head.putInt(json.length);
        head.put(json);
        head.position(head.capacity()); // the rest of the region stays zero padding
        head.flip();
        while (head.hasRemaining()) channel.write(head);
    }

    /// The compressor is closed before the header is rewritten, and closing a channel-backed
    /// stream closes the channel with it, so the chain stops one layer short of the channel.
    private static final class KeepOpen extends OutputStream {

        private final OutputStream out;

        KeepOpen(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int value) throws IOException {
            out.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            out.write(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.flush();
        }
    }

    private static void closeQuietly(@Nullable FileChannel channel) {
        if (channel == null) return;
        try {
            channel.close();
        } catch (IOException _) {
            // Already failing; the open error is the one worth reporting.
        }
    }
}

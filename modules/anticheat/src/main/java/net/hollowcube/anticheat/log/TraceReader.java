package net.hollowcube.anticheat.log;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// Reads a trace back: the header and the start-snapshot sections eagerly, the frames one at a
/// time so a dump never holds a whole capture in memory.
///
/// Truncation is expected, not exceptional. A body that ends mid-frame (or mid-chunk) leaves
/// [#truncated()] set and yields everything complete before the cut; only the fixed head and the
/// header have to be intact, because without them there is nothing to interpret.
public final class TraceReader implements AutoCloseable {

    private final InputStream file;
    private final PushbackInputStream stream;
    private final DataInputStream body;
    private final TraceHeader header;
    private final List<Frame> prelude;
    private final List<WorldChunk> chunks;

    private boolean truncated;
    private boolean ended;
    private long lastTNs;

    public static TraceReader open(Path path) {
        InputStream file = null;
        try {
            file = new BufferedInputStream(Files.newInputStream(path), 1 << 16);
            return new TraceReader(file);
        } catch (IOException e) {
            closeQuietly(file);
            throw new UncheckedIOException("failed to read trace " + path, e);
        } catch (RuntimeException e) {
            closeQuietly(file);
            throw e;
        }
    }

    /// Reads the whole trace, frames included.
    public static Trace read(Path path) {
        try (var reader = open(path)) {
            var frames = new ArrayList<Frame>();
            for (var frame = reader.nextFrame(); frame != null; frame = reader.nextFrame()) frames.add(frame);
            return new Trace(reader.header(), reader.prelude(), reader.chunks(), List.copyOf(frames),
                reader.truncated());
        }
    }

    /// Reads only the header, without touching the compressed body — what indexing and `ls`-style
    /// tooling wants, and cheap enough to run over a whole directory.
    public static TraceHeader header(Path path) {
        try (var file = new BufferedInputStream(Files.newInputStream(path), 1 << 12)) {
            return readHeader(new DataInputStream(file));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read trace header " + path, e);
        }
    }

    private TraceReader(InputStream file) throws IOException {
        this.file = file;
        this.header = readHeader(new DataInputStream(file));
        var zstd = new ZstdInputStreamNoFinalizer(file);
        var dictionary = TraceDictionary.decompress(this.header.dictionaryId());
        if (dictionary != null) zstd.setDict(dictionary);
        this.stream = new PushbackInputStream(zstd, 1);
        this.body = new DataInputStream(this.stream);

        var prelude = new ArrayList<Frame>();
        var chunks = new ArrayList<WorldChunk>();
        try {
            int preludeCount = TraceFormat.readVarInt(this.body);
            for (int i = 0; i < preludeCount; i++) {
                var frame = Frame.decode(this.body, lastTNs);
                lastTNs = frame.tNs();
                prelude.add(frame);
            }
            int chunkCount = TraceFormat.readVarInt(this.body);
            for (int i = 0; i < chunkCount; i++) chunks.add(WorldChunk.decode(this.body));
        } catch (IOException e) {
            // The body was cut before the frames even started; keep what parsed and stop there.
            this.truncated = true;
            this.ended = true;
        }
        this.prelude = List.copyOf(prelude);
        this.chunks = List.copyOf(chunks);
    }

    public TraceHeader header() {
        return header;
    }

    public List<Frame> prelude() {
        return prelude;
    }

    public List<WorldChunk> chunks() {
        return chunks;
    }

    /// The next frame, or null at the end of the body — which is either the end of the capture or
    /// the point it was cut, distinguished by [#truncated()].
    public @Nullable Frame nextFrame() {
        if (ended) return null;
        try {
            // The frame section runs to the end of the body, so the only clean end is an end of
            // stream where a frame would have started. Anything else — a short frame, or zstd
            // refusing a frame the writer never finished — is a cut.
            int next = stream.read();
            if (next == -1) {
                ended = true;
                return null;
            }
            stream.unread(next);
            var frame = Frame.decode(body, lastTNs);
            lastTNs = frame.tNs();
            return frame;
        } catch (IOException e) {
            ended = true;
            truncated = true;
            return null;
        }
    }

    public boolean truncated() {
        return truncated;
    }

    @Override
    public void close() {
        closeQuietly(file);
    }

    private static TraceHeader readHeader(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != TraceFormat.MAGIC)
            throw new TraceFormatException("not a trace file: magic " + Integer.toHexString(magic));

        int version = in.readUnsignedShort();
        if (version < TraceFormat.VERSION_OLDEST_READABLE || version > TraceFormat.VERSION_LATEST) {
            throw new TraceFormatException("unsupported trace format version " + version
                + ", this build reads " + TraceFormat.VERSION_OLDEST_READABLE + ".."
                + TraceFormat.VERSION_LATEST);
        }

        int capacity = in.readInt();
        int length = in.readInt();
        if (capacity < 0 || capacity > TraceFormat.MAX_HEADER_LENGTH || length < 0 || length > capacity)
            throw new TraceFormatException("corrupt trace header region: " + length + " of " + capacity);

        var json = new byte[length];
        in.readFully(json);
        in.skipNBytes(capacity - (long) length);
        return TraceHeader.fromJson(new String(json, StandardCharsets.UTF_8));
    }

    private static void closeQuietly(@Nullable InputStream in) {
        if (in == null) return;
        try {
            in.close();
        } catch (IOException _) {
            // Nothing was written; a failed close on a read is not worth reporting.
        }
    }
}

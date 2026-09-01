package net.hollowcube.anticheat.log;

import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// The zstd dictionaries a trace body may be compressed against, by the id its header carries.
///
/// A trace is one zstd stream of packets whose framing repeats across every connection, and at the
/// sizes captures actually run to there is not enough of it in one file for the compressor to
/// learn from: measured over 776 production traces, a dictionary is -55% on the 64-256KB band the
/// median trace sits in and -28% overall, against -14% for raising the level instead.
///
/// Ids rather than a single built-in dictionary because retraining produces a different dictionary
/// and every trace ever written has to stay readable: a new one is a new id and a new resource,
/// [#LATEST] moves, and the old resource stays where it is forever. [#NONE] is the traces written
/// before any of this, whose bodies are plain zstd.
public final class TraceDictionary {

    public static final int NONE = 0;

    /// Trained with `zstd --train --maxdict=112640` over 200 production traces from 2026-09-01.
    /// The gain saturates by about 25 samples, so a retrain wants breadth (client brands, maps)
    /// rather than volume.
    public static final int LATEST = 1;

    private static final Map<Integer, byte[]> BYTES = new ConcurrentHashMap<>();
    private static final Map<Integer, ZstdDictCompress> COMPRESS = new ConcurrentHashMap<>();
    private static final Map<Integer, ZstdDictDecompress> DECOMPRESS = new ConcurrentHashMap<>();

    /// The compressor-side dictionary, or null for [#NONE]. Shared: zstd's digested forms are
    /// immutable and every connection on a proxy writes against the same one.
    public static @Nullable ZstdDictCompress compress(int id) {
        if (id == NONE) return null;
        return COMPRESS.computeIfAbsent(id, key ->
            new ZstdDictCompress(bytes(key), TraceFormat.COMPRESSION_LEVEL));
    }

    public static @Nullable ZstdDictDecompress decompress(int id) {
        if (id == NONE) return null;
        return DECOMPRESS.computeIfAbsent(id, key -> new ZstdDictDecompress(bytes(key)));
    }

    private static byte[] bytes(int id) {
        return BYTES.computeIfAbsent(id, key -> {
            var resource = "trace-" + key + ".zdict";
            try (var in = TraceDictionary.class.getResourceAsStream(resource)) {
                if (in == null) throw new TraceFormatException("no trace dictionary " + key);
                return in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException("failed to read trace dictionary " + key, e);
            }
        });
    }

    private TraceDictionary() {}
}

package net.hollowcube.apiserver.anticheat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Pattern;

/// The capture trace blobs on the shared volume, laid out as `{yyyy}/{mm}/{dd}/{id}.trace`.
///
/// Dated by when the capture started rather than by when it arrived, so that the ship retries the
/// proxy does for up to ten minutes always name the same file: a retry that crosses midnight
/// rewrites the blob it already wrote instead of leaving a second copy a day later, and the row
/// keeps pointing at the one path it was ever given.
///
/// Every write lands on a temp name beside its target and is renamed into place. Readers share the
/// mount with writers, and a `.trace` still being written is indistinguishable from one the proxy
/// was killed halfway through — the reader handles truncation, but only after it has decided the
/// file is worth opening, and that decision is what the rename protects.
public final class AnticheatTraceStore {

    /// `HCTR`, "hollow cube trace", then the container's `u16` format version. The rest of the
    /// head is the trace reader's business; this is the prefix that says the body is one at all.
    private static final byte[] MAGIC = {'H', 'C', 'T', 'R'};
    private static final int PREFIX_LENGTH = MAGIC.length + 2;

    /// A trace id is a path component, and nothing proves the proxy is the only thing that can
    /// PUT here. The leading character rules out `..` without a second rule about it.
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final int BUFFER_BYTES = 1 << 16;

    private final Path root;
    private final long maxBytes;

    public AnticheatTraceStore(Path root, long maxBytes) {
        this.root = root.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public static boolean validId(String id) {
        return ID.matcher(id).matches();
    }

    /// Where a trace that started at `startedAt` belongs, relative to the root.
    public static String pathOf(String id, Instant startedAt) {
        return DATE.format(startedAt) + "/" + id + ".trace";
    }

    /// The file a stored path names, refusing one that would climb out of the root.
    public Path resolve(String path) {
        var resolved = root.resolve(path).normalize();
        if (!resolved.startsWith(root))
            throw new IllegalArgumentException("trace path leaves the store: " + path);
        return resolved;
    }

    /// Streams `body` onto the volume at `path`, refusing anything that is not a trace or is
    /// longer than the store accepts. A refusal leaves nothing behind, including the temp name.
    public Result write(String path, InputStream body) throws IOException {
        var target = resolve(path);
        Files.createDirectories(target.getParent());
        var temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        var moved = false;
        try {
            var prefix = new byte[PREFIX_LENGTH];
            var prefixLength = 0;
            var written = 0L;
            try (var out = Files.newOutputStream(temp)) {
                var buffer = new byte[BUFFER_BYTES];
                for (int read; (read = body.read(buffer)) != -1; ) {
                    // Before the write rather than after it: the cap is on what reaches the disk,
                    // and the rest of the body is not read at all.
                    if (written + read > maxBytes) return Result.TooLarge.INSTANCE;
                    var keep = Math.min(read, PREFIX_LENGTH - prefixLength);
                    if (keep > 0) {
                        System.arraycopy(buffer, 0, prefix, prefixLength, keep);
                        prefixLength += keep;
                    }
                    out.write(buffer, 0, read);
                    written += read;
                }
            }
            if (prefixLength < PREFIX_LENGTH || !Arrays.equals(prefix, 0, MAGIC.length, MAGIC, 0, MAGIC.length))
                return Result.NotATrace.INSTANCE;

            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            moved = true;
            return new Result.Stored(written, (prefix[4] & 0xFF) << 8 | prefix[5] & 0xFF);
        } finally {
            if (!moved) Files.deleteIfExists(temp);
        }
    }

    /// What a [#write] did.
    public sealed interface Result {

        /// `formatVersion` is the container's, read off the bytes rather than taken on trust from
        /// the header the proxy sent alongside them.
        record Stored(long bytes, int formatVersion) implements Result {
        }

        /// The body did not open with the magic, so it is not a trace and not worth keeping.
        record NotATrace() implements Result {

            public static final NotATrace INSTANCE = new NotATrace();
        }

        /// Longer than the store accepts.
        record TooLarge() implements Result {

            public static final TooLarge INSTANCE = new TooLarge();
        }
    }
}

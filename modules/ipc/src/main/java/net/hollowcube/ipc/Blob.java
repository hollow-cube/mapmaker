package net.hollowcube.ipc;

import net.hollowcube.ipc.util.IpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/// The one thing an ipc call carries as bytes rather than as json: a whole request or response body.
///
/// A capture trace or a replay is hundreds of megabytes, so neither side ever holds one — the
/// client sends the stream as it reads it and the server writes the stream as it arrives. That is
/// also why a blob may only be a whole body: it is the request or the response, never a field of
/// one, and a method takes at most one.
///
/// The stream is whoever holds it last: a blob handed to a call is read and closed as it is sent, a
/// blob a call answers with is still open on the socket and the caller closes it.
///
/// @param length the byte count, announced as the body's content-length; negative when nobody knows
///               it, which is what makes the body chunked
public record Blob(long length, InputStream stream) implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Blob.class);
    private static final int BUFFER_BYTES = 1 << 16;

    public static Blob of(byte[] bytes) {
        return new Blob(bytes.length, new ByteArrayInputStream(bytes));
    }

    /// The file as it is now; a write that lands after this call is not in [#length].
    public static Blob of(Path file) throws IOException {
        return new Blob(Files.size(file), Files.newInputStream(file));
    }

    /// The whole body in memory, closing the stream. For a blob small enough to want that — a test,
    /// or a caller that is about to parse it — and not for the ones this type exists for.
    public byte[] readAllBytes() throws IOException {
        try (this) {
            return stream.readAllBytes();
        }
    }

    /// [#length], refusing a body that did not announce one — a caller that has to size the body up
    /// front, to check a cap or declare an upload length, has nothing to work with otherwise.
    public long requireLength() {
        if (length < 0) throw new IpcException(411, "this body must announce its length");
        return length;
    }

    /// Exactly `count` bytes off the front, or a 400 naming what fell short. `readNBytes` grows as
    /// it reads, so a body that stops early costs what arrived and not what it claimed.
    public byte[] read(int count, String what) throws IOException {
        var bytes = stream.readNBytes(count);
        if (bytes.length != count)
            throw new IpcException(400, what + " ended after " + bytes.length + " of " + count + " bytes");
        return bytes;
    }

    /// Reads and throws away whatever is left, so a caller being refused gets its status rather than
    /// a connection dropped while it is still writing. A body that announced no length is left
    /// alone: holding the handler open on an unbounded stream is worse than closing it.
    public void drain() {
        if (length < 0) return;
        try {
            var buffer = new byte[BUFFER_BYTES];
            var read = 0L;
            for (int n; read < length && (n = stream.read(buffer)) != -1; ) read += n;
        } catch (IOException e) {
            logger.debug("draining a refused body failed", e);
        }
    }

    /// Written as `throw body.refuse(...)` so the refusal and the [#drain] cannot come apart.
    public IpcException refuse(int status, String message) {
        drain();
        return new IpcException(status, message);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}

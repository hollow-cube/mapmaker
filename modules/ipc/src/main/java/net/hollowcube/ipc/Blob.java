package net.hollowcube.ipc;

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

    @Override
    public void close() throws IOException {
        stream.close();
    }
}

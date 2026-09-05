package net.hollowcube.apiserver.s3;

import net.hollowcube.ipc.Blob;

import java.io.InputStream;
import java.util.List;

/// One bucket, as the five operations the api needs of it.
///
/// Named for the protocol rather than the vendor: this speaks the S3 API, and what answers it is R2
/// in the cluster and MinIO on a laptop.
public interface S3Client {

    /// `length` is announced so the far side can size the write; the stream is read once and closed.
    void put(String key, InputStream body, long length);

    Blob get(String key);

    /// The inclusive range, whose [Blob#length] is the range's rather than the object's.
    Blob getRange(String key, long start, long endInclusive);

    /// Idempotent, including for a key that is not there, which is what lets an interrupted sweep
    /// simply run again.
    void delete(String key);

    /// Every key under `prefix`, following the continuation tokens.
    List<String> list(String prefix);

    /// Raised only by [#get] and [#getRange]; whether a missing object is a 404 or a corruption is
    /// the caller's to decide.
    final class NotFoundError extends RuntimeException {

        public NotFoundError(String key) {
            super("no object " + key);
        }
    }

    /// Anything the store answered that is not a success and not a 404.
    final class RequestFailedError extends RuntimeException {

        public RequestFailedError(String message) {
            super(message);
        }

        public RequestFailedError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package net.hollowcube.ipc.util;

import org.jetbrains.annotations.Nullable;

/// Thrown by a generated ipc client when a call does not answer 2xx.
///
/// The only type the generated code needs at runtime beyond Gson and the JDK, which is what lets
/// `*Http`/`*Handler` be dropped into any process without dragging a transport framework along.
public class IpcException extends RuntimeException {
    private final int status;
    private final @Nullable String call;

    public IpcException(int status, String message) {
        this(status, null, message, null);
    }

    public IpcException(int status, String message, Throwable cause) {
        this(status, null, message, cause);
    }

    public IpcException(
        int status,
        @Nullable String call,
        String message,
        @Nullable Throwable cause
    ) {
        super(message, cause);
        this.status = status;
        this.call = call;
    }

    public static IpcException badRequest(String message) {
        return new IpcException(400, message);
    }

    public static IpcException notFound(String message) {
        return new IpcException(404, message);
    }

    public static IpcException conflict(String message) {
        return new IpcException(409, message);
    }

    public static IpcException unprocessable(String message) {
        return new IpcException(422, message);
    }

    public static IpcException internal(String message) {
        return new IpcException(500, message);
    }

    public static IpcException unavailable(String message) {
        return new IpcException(503, message);
    }

    /// The status the remote answered with, or zero if the call never got one.
    public int status() {
        return status;
    }

    /// The ipc path a generated client was calling, like `/map/get`, or null when thrown by an implementation.
    public @Nullable String call() {
        return call;
    }
}

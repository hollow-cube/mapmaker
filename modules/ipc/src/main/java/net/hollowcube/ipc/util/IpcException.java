package net.hollowcube.ipc.util;

/// Thrown by a generated ipc client when a call does not answer 2xx.
///
/// The only type the generated code needs at runtime beyond Gson and the JDK, which is what lets
/// `*Http`/`*Handler` be dropped into any process without dragging a transport framework along.
public class IpcException extends RuntimeException {
    private final int status;

    public IpcException(int status, String message) {
        super(message);
        this.status = status;
    }

    public IpcException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /// The status the remote answered with, or zero if the call never got one.
    public int status() {
        return status;
    }
}

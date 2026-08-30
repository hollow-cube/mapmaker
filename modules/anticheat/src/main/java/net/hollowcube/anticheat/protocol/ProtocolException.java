package net.hollowcube.anticheat.protocol;

/// Thrown when a frame cannot be read as the packet the registry says it is. Always per-packet:
/// the tap drops the frame and keeps the connection.
public final class ProtocolException extends RuntimeException {
    public ProtocolException(String message) {
        super(message);
    }
}

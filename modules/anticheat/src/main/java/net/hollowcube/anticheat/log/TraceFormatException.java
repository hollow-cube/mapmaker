package net.hollowcube.anticheat.log;

/// A trace file that is not one, or is one this build cannot read.
///
/// Distinct from an [java.io.IOException]: the bytes arrived, they just do not describe a trace
/// this reader understands, which is a permanent failure rather than something to retry.
public final class TraceFormatException extends RuntimeException {

    public TraceFormatException(String message) {
        super(message);
    }
}

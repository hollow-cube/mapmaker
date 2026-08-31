package net.hollowcube.anticheat.control;

import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.log.TraceHeader;
import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

/// A message on the `mapmaker:anticheat` plugin channel: one byte of kind, then a gson body.
///
/// The channel is one-way — the backend sends [Start], [Stop] and [Flush], and the proxy answers
/// nothing: a shipped trace is found in the store by capture id, which is reliable where a plugin
/// message to a player who may already be gone is not. It lives in this module because the backend
/// and the proxy are on opposite sides of a deploy boundary and this is the one thing they both
/// read.
///
/// The reason and cohort spellings come from [TraceHeader] rather than a second set of enums,
/// since they are the same words the trace header carries and gson writes them the same way.
///
/// Every field is optional as far as parsing goes, so an older proxy talking to a newer backend
/// loses fields rather than the message; what the fields must actually be is the reader's business.
public sealed interface CaptureControl {

    byte KIND_START = 0;
    byte KIND_STOP = 1;
    byte KIND_FLUSH = 2;

    /// Opens a capture on the player's connection. A second start while one is open supersedes it.
    ///
    /// @param captureId what the trace is filed under; the run id for a compete run
    /// @param cohort    null unless this is a sample, where it labels how much the grader trusts it
    @RuntimeGson
    record Start(@Nullable String captureId, @Nullable TraceHeader.Reason reason,
                 @Nullable TraceHeader.Cohort cohort, @Nullable TrimPolicy trim) implements CaptureControl {

        @Override
        public byte kind() {
            return KIND_START;
        }
    }

    /// Closes the capture with this id, if it is the one the proxy has open.
    @RuntimeGson
    record Stop(@Nullable String captureId) implements CaptureControl {

        @Override
        public byte kind() {
            return KIND_STOP;
        }
    }

    /// Ships the ring buffer without disturbing an active capture. The capture id is whatever
    /// capture is open at the time, and null when there is none.
    @RuntimeGson
    record Flush(@Nullable String captureId, @Nullable TraceHeader.Reason reason) implements CaptureControl {

        @Override
        public byte kind() {
            return KIND_FLUSH;
        }
    }

    byte kind();

    default byte[] encode() {
        var body = ControlJson.GSON.toJson(this).getBytes(StandardCharsets.UTF_8);
        var message = new byte[body.length + 1];
        message[0] = kind();
        System.arraycopy(body, 0, message, 1, body.length);
        return message;
    }

    static CaptureControl decode(byte[] message) {
        if (message.length == 0) throw new IllegalArgumentException("empty control message");
        var body = new String(message, 1, message.length - 1, StandardCharsets.UTF_8);
        CaptureControl decoded = switch (message[0]) {
            case KIND_START -> ControlJson.GSON.fromJson(body, Start.class);
            case KIND_STOP -> ControlJson.GSON.fromJson(body, Stop.class);
            case KIND_FLUSH -> ControlJson.GSON.fromJson(body, Flush.class);
            default -> throw new IllegalArgumentException("unknown control message kind: " + message[0]);
        };
        if (decoded == null) throw new IllegalArgumentException("empty control message body");
        return decoded;
    }
}

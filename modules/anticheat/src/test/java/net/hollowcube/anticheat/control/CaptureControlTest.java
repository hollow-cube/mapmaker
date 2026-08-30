package net.hollowcube.anticheat.control;

import net.hollowcube.anticheat.capture.TrimPolicy;
import net.hollowcube.anticheat.log.TraceHeader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// The control channel's wire format, byte for byte. The backend and the proxy read these bytes on
/// opposite sides of a deploy boundary, so a change here is a change on both.
class CaptureControlTest {

    @Test
    void testStartCarriesTheCaptureIdReasonCohortAndTrim() {
        assertEncodes(new CaptureControl.Start("run-1", TraceHeader.Reason.RUN, TraceHeader.Cohort.TRUSTED,
                TrimPolicy.DEFAULT), CaptureControl.KIND_START,
            "{\"captureId\":\"run-1\",\"reason\":\"run\",\"cohort\":\"trusted\",\"trim\":{\"chunkRadius\":2,\"entityRange\":8}}");
    }

    @Test
    void testStartWithoutACohortLeavesItOut() {
        assertEncodes(new CaptureControl.Start("run-1", TraceHeader.Reason.MANUAL, null, TrimPolicy.EVERYTHING),
            CaptureControl.KIND_START,
            "{\"captureId\":\"run-1\",\"reason\":\"manual\",\"trim\":{\"chunkRadius\":-1,\"entityRange\":8}}");
    }

    @Test
    void testStopIsNothingButTheCaptureId() {
        assertEncodes(new CaptureControl.Stop("run-1"), CaptureControl.KIND_STOP, "{\"captureId\":\"run-1\"}");
    }

    @Test
    void testFlushWithNoCaptureOpenCarriesOnlyTheReason() {
        assertEncodes(new CaptureControl.Flush(null, TraceHeader.Reason.MANUAL), CaptureControl.KIND_FLUSH,
            "{\"reason\":\"manual\"}");
        assertEncodes(new CaptureControl.Flush("run-1", TraceHeader.Reason.FLAG), CaptureControl.KIND_FLUSH,
            "{\"captureId\":\"run-1\",\"reason\":\"flag\"}");
    }

    /// A backend that grew a field the proxy has never heard of still gets its message across.
    @Test
    void testAStartFromANewerBackendLosesFieldsRatherThanTheMessage() {
        var message = message(CaptureControl.KIND_START,
            "{\"captureId\":\"run-1\",\"reason\":\"sample\",\"mood\":\"suspicious\"}");

        assertEquals(new CaptureControl.Start("run-1", TraceHeader.Reason.SAMPLE, null, null),
            CaptureControl.decode(message));
    }

    /// Enum spellings are the trace header's, and one the proxy does not know is a missing field
    /// rather than a refusal.
    @Test
    void testAnUnknownReasonDecodesAsNone() {
        var decoded = CaptureControl.decode(message(CaptureControl.KIND_FLUSH, "{\"reason\":\"vibes\"}"));

        assertNull(((CaptureControl.Flush) decoded).reason());
    }

    @Test
    void testAnUnknownKindIsRefusedRatherThanGuessed() {
        assertThrows(IllegalArgumentException.class, () -> CaptureControl.decode(new byte[]{99, '{', '}'}));
        assertThrows(IllegalArgumentException.class, () -> CaptureControl.decode(new byte[0]));
    }

    private static void assertEncodes(CaptureControl message, byte kind, String json) {
        var encoded = message.encode();
        assertEquals(kind, encoded[0]);
        assertEquals(json, new String(encoded, 1, encoded.length - 1, StandardCharsets.UTF_8));
        assertEquals(message, CaptureControl.decode(encoded));
    }

    private static byte[] message(byte kind, String body) {
        var json = body.getBytes(StandardCharsets.UTF_8);
        var message = new byte[json.length + 1];
        message[0] = kind;
        System.arraycopy(json, 0, message, 1, json.length);
        return message;
    }
}

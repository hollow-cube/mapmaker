package net.hollowcube.ipc.replay;

import com.google.gson.JsonParser;
import net.hollowcube.ipc.Wire;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The replay wire as JSON, pinned.
///
/// A recording started on one tag and resumed on another is the normal case — a save state moves
/// between servers mid-run — so these names outlive every deploy and a rename here is a break.
class ReplayWireTest {

    @Test
    void replayInfo_roundTripsAndSaysWhatIsAbsentByLeavingItOut() {
        var info = new ReplayInfo("r1", 7, ReplayState.RECORDING, ReplayRepresentation.SEGMENTED,
            3, 512, null, 1756000000000L);

        var json = Wire.gson().toJson(info);

        assertEquals(JsonParser.parseString("""
            {"id":"r1","revision":7,"state":"RECORDING","representation":"SEGMENTED",
             "nextSegmentIndex":3,"preambleLength":512,"updatedAt":1756000000000}"""),
            JsonParser.parseString(json));
        assertEquals(info, Wire.gson().fromJson(json, ReplayInfo.class));
    }

    @Test
    void replayInfo_compactedHasNoNextSegmentIndexAndKeepsItsOutcome() {
        var info = new ReplayInfo("r1", 9, ReplayState.FINISHED, ReplayRepresentation.COMPACTED,
            null, 900, ReplayOutcome.RESET, 1756000000000L);

        var json = Wire.gson().toJson(info);

        assertNull(JsonParser.parseString(json).getAsJsonObject().get("nextSegmentIndex"));
        assertEquals("RESET", JsonParser.parseString(json).getAsJsonObject().get("outcome").getAsString());
        assertEquals(info, Wire.gson().fromJson(json, ReplayInfo.class));
    }

    @Test
    void replayCommit_roundTripsBothShapesOfACommit() {
        var appending = new ReplayCommit("r1", 7L, "k-1", 512, 3, false, null, "3q2+7w==");
        assertEquals(appending, Wire.gson().fromJson(Wire.gson().toJson(appending), ReplayCommit.class));

        // The metadata-only final commit: no segment, and an outcome because it is final.
        var finishing = new ReplayCommit("r1", 8L, "k-2", 512, null, true, ReplayOutcome.FINISHED, "3q2+7w==");
        assertEquals(finishing, Wire.gson().fromJson(Wire.gson().toJson(finishing), ReplayCommit.class));

        // Creating: no revision at all is what says so, rather than a sentinel.
        var creating = new ReplayCommit("r1", null, "k-0", 256, 0, false, null, "3q2+7w==");
        var json = Wire.gson().toJson(creating);
        assertNull(JsonParser.parseString(json).getAsJsonObject().get("expectedRevision"));
        assertEquals(creating, Wire.gson().fromJson(json, ReplayCommit.class));
    }

    @Test
    void replayCompaction_roundTrips() {
        var compaction = new ReplayCompaction("r1", 8, "compact:r1:8", 900, "3q2+7w==");
        assertEquals(compaction, Wire.gson().fromJson(Wire.gson().toJson(compaction), ReplayCompaction.class));
    }

    /// A revision and a timestamp are both `long` on the wire, and gson writes a json number for
    /// each. Nothing here is allowed to go through a `double` on the way — 2^53 is where that would
    /// start silently rounding, and an epoch-millisecond timestamp is already a third of the way to
    /// it — so this pins a value that only survives if the whole path is integral.
    @Test
    void longs_crossTheWireExactlyRatherThanThroughADouble() {
        var info = new ReplayInfo("r1", Long.MAX_VALUE, ReplayState.FINISHED,
            ReplayRepresentation.COMPACTED, null, 900, ReplayOutcome.RESET, Long.MAX_VALUE - 1);

        var json = Wire.gson().toJson(info);

        assertEquals("9223372036854775807", JsonParser.parseString(json).getAsJsonObject()
            .get("revision").getAsBigInteger().toString());
        assertEquals(info, Wire.gson().fromJson(json, ReplayInfo.class));
        // The generated client and server move arguments through a JsonElement rather than text,
        // which is the other path a number could lose precision on.
        assertEquals(info, Wire.gson().fromJson(Wire.gson().toJsonTree(info, ReplayInfo.class), ReplayInfo.class));
    }

    @Test
    void enums_readAConstantThisBuildDoesNotKnowAsUnknown() {
        assertEquals(ReplayState.UNKNOWN, Wire.gson().fromJson("\"ABANDONED\"", ReplayState.class));
        assertEquals(ReplayRepresentation.UNKNOWN, Wire.gson().fromJson("\"SHARDED\"", ReplayRepresentation.class));
        assertEquals(ReplayOutcome.UNKNOWN, Wire.gson().fromJson("\"CRASHED\"", ReplayOutcome.class));

        assertEquals(ReplayState.FINISHED, Wire.gson().fromJson("\"FINISHED\"", ReplayState.class));
        assertEquals(ReplayOutcome.RESET, Wire.gson().fromJson("\"RESET\"", ReplayOutcome.class));
    }
}

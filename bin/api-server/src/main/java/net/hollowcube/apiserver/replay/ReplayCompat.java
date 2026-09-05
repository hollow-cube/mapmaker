package net.hollowcube.apiserver.replay;

import com.google.gson.JsonObject;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.apiserver.db.Replays;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/// What the Go api-server and this one have to spell identically: the column values, the object
/// keys, and what decides whether a retry is the same request.
///
/// The two are not implementations behind a switch, they are two front doors onto one database and
/// one bucket, and a save state moves between servers on different tags mid-recording. Everything
/// here is transcribed from `api/v4Internal/server_replays.go`; changing any of it breaks a
/// recording that spans the two, silently.
final class ReplayCompat {

    static final String RECORDING = "recording";
    static final String FINISHED = "finished";
    static final String SEGMENTED = "segmented";
    static final String COMPACTED = "compacted";

    private ReplayCompat() {
    }

    /// The revision as Go writes it down: an HTTP entity tag, quotes included. Nothing on the wire
    /// carries this; it survives only because Go stores it in `replay_idempotency.response_etag` and
    /// hashes it into the fingerprint.
    static String etag(long version) {
        return "\"r" + version + "\"";
    }

    /// Back out of [#etag], since an idempotency record stores the Go spelling whoever wrote it.
    static long revision(String etag) {
        var digits = etag.replace("\"", "");
        if (digits.startsWith("r")) digits = digits.substring(1);
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw new IpcException(500, "stored replay etag is not a revision: " + etag);
        }
    }

    /// A fresh staging key per call. The id is hashed because it is client-supplied and never
    /// validated as a uuid, so it must not be able to escape the `replays/` prefix.
    static String objectKey(String replayId, String kind) {
        return "replays/" + Digest.hex(Digest.sha256(replayId)) + "/" + kind + "/" + UUID.randomUUID();
    }

    /// What makes one write distinct from another under the same idempotency key.
    ///
    /// Length-prefixed so no combination of values can collide by running into its neighbour, and
    /// the length is the **UTF-8 byte** count: Go writes `len(value)`, which counts bytes, so a
    /// non-ASCII replay id fingerprints differently if this counts chars.
    static byte[] fingerprint(String method, String replayId, String ifMatch, String ifNoneMatch,
                              String preambleLength, String segmentIndex, String finished, byte[] bodyDigest) {
        var out = new ByteArrayOutputStream();
        for (var field : new String[] {method, replayId, ifMatch, ifNoneMatch, preambleLength,
            segmentIndex, finished, Digest.base64(bodyDigest)}) {
            var bytes = field.getBytes(StandardCharsets.UTF_8);
            out.writeBytes((bytes.length + ":").getBytes(StandardCharsets.UTF_8));
            out.writeBytes(bytes);
        }
        return Digest.sha256(out.toByteArray());
    }

    /// Go's `validateStoredReplay`. A row that fails these was written by something with a bug, and
    /// serving it would spread the damage.
    static void validate(Replays row) {
        if (!RECORDING.equals(row.state()) && !FINISHED.equals(row.state()))
            throw new IpcException(500, "stored replay " + row.id() + " has invalid state " + row.state());
        switch (row.representation()) {
            case SEGMENTED -> {
                if (row.nextSegmentIndex() < 0)
                    throw new IpcException(500, "stored replay " + row.id() + " has invalid next segment index");
            }
            case COMPACTED -> {
                if (!FINISHED.equals(row.state()) || row.compactedObject() == null
                    || row.compactedLength() == null || row.compactedDigest() == null)
                    throw new IpcException(500, "stored replay " + row.id() + " has invalid compacted state");
            }
            default -> throw new IpcException(500,
                "stored replay " + row.id() + " has invalid representation " + row.representation());
        }
        if (row.currentPreambleDigest().length != 32)
            throw new IpcException(500, "stored replay " + row.id() + " has invalid preamble digest");
    }

    static ReplayInfo info(Replays row) {
        return new ReplayInfo(row.id(), row.version(), state(row.state()), representation(row.representation()),
            SEGMENTED.equals(row.representation()) ? (int) row.nextSegmentIndex() : null,
            row.currentPreamble().length, outcome(row.outcome()), row.updatedAt().toEpochMilli());
    }

    /// The `response_metadata` column. Go writes and reads the first three; the rest are ours, and
    /// `encoding/json` ignores what it does not know, so a record written here still replays through
    /// the Go handler. Built by hand because the names are a compatibility surface and this is a
    /// native image.
    static JsonObject recorded(Replays row) {
        var out = new JsonObject();
        out.addProperty("state", row.state());
        out.addProperty("representation", row.representation());
        if (SEGMENTED.equals(row.representation())) out.addProperty("nextSegmentIndex", row.nextSegmentIndex());
        out.addProperty("preambleLength", row.currentPreamble().length);
        if (row.outcome() != null) out.addProperty("outcome", row.outcome());
        out.addProperty("updatedAt", row.updatedAt().toEpochMilli());
        return out;
    }

    static ReplayState state(@Nullable String value) {
        return switch (value == null ? "" : value) {
            case RECORDING -> ReplayState.RECORDING;
            case FINISHED -> ReplayState.FINISHED;
            default -> ReplayState.UNKNOWN;
        };
    }

    static ReplayRepresentation representation(@Nullable String value) {
        return switch (value == null ? "" : value) {
            case SEGMENTED -> ReplayRepresentation.SEGMENTED;
            case COMPACTED -> ReplayRepresentation.COMPACTED;
            default -> ReplayRepresentation.UNKNOWN;
        };
    }

    static @Nullable ReplayOutcome outcome(@Nullable String value) {
        if (value == null) return null;
        return switch (value) {
            case FINISHED -> ReplayOutcome.FINISHED;
            case "reset" -> ReplayOutcome.RESET;
            default -> ReplayOutcome.UNKNOWN;
        };
    }

    /// The column value, or null for a commit that is not finishing the recording.
    static @Nullable String outcome(boolean finished, @Nullable ReplayOutcome outcome) {
        if (!finished || outcome == null) return null;
        return switch (outcome) {
            case FINISHED -> FINISHED;
            case RESET -> "reset";
            case UNKNOWN -> throw new IpcException(400, "unknown replay outcome");
        };
    }
}

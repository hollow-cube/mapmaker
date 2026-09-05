package net.hollowcube.apiserver.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.hollowcube.apiserver.common.Digest;
import net.hollowcube.apiserver.s3.S3Client;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.ReplayIdempotency;
import net.hollowcube.apiserver.db.Replays;
import net.hollowcube.apiserver.db.ReplaysQueries;
import net.hollowcube.apiserver.job.CompactReplay;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.replay.ReplayCommit;
import net.hollowcube.ipc.replay.ReplayCompaction;
import net.hollowcube.ipc.replay.ReplayInfo;
import net.hollowcube.ipc.replay.ReplayOutcome;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.hollowcube.apiserver.replay.ReplayCompat.COMPACTED;
import static net.hollowcube.apiserver.replay.ReplayCompat.FINISHED;
import static net.hollowcube.apiserver.replay.ReplayCompat.SEGMENTED;
import static net.hollowcube.apiserver.replay.ReplayCompat.info;
import static net.hollowcube.apiserver.replay.ReplayCompat.validate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/// Replay storage: the rows in Postgres, the segments and compacted objects in the bucket.
///
/// Nothing here reads a replay. Every decision — whether this commit may append, which segment is
/// next, whether the recording is over — is made from the row and the metadata alone.
///
/// The Go api-server serves the same tables and bucket for as long as both are up, so everything in
/// [ReplayCompat] is a constraint rather than a choice. Four things this does that Go does not, all
/// fixes: a body of unannounced length is refused rather than pre-allocated; a retry that matches
/// an idempotency record uploads nothing; a staged object is removed on every path that then
/// rejects the write; and losing the create race answers 412 rather than 500.
public final class ReplayServiceImpl implements ReplayService {

    private static final Logger logger = LoggerFactory.getLogger(ReplayServiceImpl.class);


    /// Go's `replay:` block in `config/default.yaml`. Generous against what production produces —
    /// the largest observed preamble is ~320 KB and the largest segment 4.2 MB — because these stop
    /// a runaway rather than shape the data.
    private static final long MAX_PREAMBLE_BYTES = 16L << 20;
    private static final long MAX_SEGMENT_BYTES = 128L << 20;
    private static final long MAX_COMMIT_BYTES = 144L << 20;
    private static final long MAX_COMPACTED_BYTES = 2L << 30;

    /// Inclusive, and 2 KiB because that is Postgres' TOAST threshold: anything larger is pushed
    /// out of line anyway, at which point object storage is cheaper. The read path branches on
    /// which column is populated and never on this, so moving it cannot misroute an old segment.
    private static final long MAX_INLINE_SEGMENT_BYTES = 2048;

    /// What the `replays.id` and `idempotency_key` constraints accept, checked here so an oversized
    /// one is a bad request naming the field rather than a constraint violation naming the table.
    private static final int MAX_ID_LENGTH = 512;

    private final ApiDatabase db;
    private final S3Client s3;

    public ReplayServiceImpl(ApiDatabase db, S3Client s3) {
        this.db = db;
        this.s3 = s3;
    }

    @Override
    public @Nullable ReplayInfo getReplay(String id) {
        var row = db.replays.getReplay(id);
        if (row == null) return null;
        validate(row);
        return info(row);
    }

    @Override
    public Blob getPreamble(String id, @Nullable Long expectedRevision) {
        var row = db.replays.getReplay(id);
        if (row == null) throw new IpcException(404, "no replay " + id);
        validate(row);
        if (expectedRevision != null && expectedRevision != row.version())
            throw new IpcException(412, "replay " + id + " is at revision " + row.version()
                + ", not " + expectedRevision);
        return Blob.of(row.currentPreamble());
    }

    @Override
    public Blob getSegment(String id, int segmentIndex) {
        if (segmentIndex < 0) throw new IpcException(400, "negative segment index: " + segmentIndex);

        var segment = db.replays.getReplaySegment(id, segmentIndex);
        if (segment == null) throw new IpcException(404, "no segment " + segmentIndex + " of replay " + id);

        if (segment.data() != null) {
            if (segment.data().length != segment.length())
                throw new IpcException(500, "replay " + id + " segment " + segmentIndex + " has an inconsistent inline length");
            return Blob.of(segment.data());
        }
        if (segment.objectReference() == null || segment.objectReference().isEmpty())
            throw new IpcException(500, "replay " + id + " segment " + segmentIndex + " has no data");
        return object(segment.objectReference(), null, null);
    }

    @Override
    public Blob getCompacted(String id, @Nullable Long start, @Nullable Long endInclusive) {
        var row = db.replays.getReplay(id);
        if (row == null) throw new IpcException(404, "no replay " + id);
        validate(row);
        if (!COMPACTED.equals(row.representation()))
            throw new IpcException(409, "replay_not_compacted");

        var length = row.compactedLength();
        if (start == null && endInclusive == null) return object(row.compactedObject(), null, null);
        if (start == null || endInclusive == null)
            throw new IpcException(400, "a replay range takes both bounds or neither");
        if (length == null || length <= 0 || start < 0 || start >= length || endInclusive < start)
            throw new IpcException(416, "replay " + id + " is " + length + " bytes; "
                + start + "-" + endInclusive + " is not in it");
        return object(row.compactedObject(), start, Math.min(endInclusive, length - 1));
    }

    @Override
    public int dropSegments(String id) {
        var row = db.replays.getReplay(id);
        if (row == null) throw new IpcException(404, "no replay " + id);
        if (!COMPACTED.equals(row.representation()))
            throw new IpcException(409, "replay_not_compacted");

        var segments = db.replays.listReplaySegmentObjects(id);
        // Objects first: an interrupted sweep then leaves rows pointing at objects that are gone,
        // which nothing reads and the next pass finishes. The other order orphans objects.
        for (var segment : segments) {
            if (segment.objectReference() != null) s3.delete(segment.objectReference());
        }
        db.replays.deleteReplaySegments(id);

        logger.info("dropped {} source segments of compacted replay {}", segments.size(), id);
        return segments.size();
    }

    @Override
    public ReplayInfo commit(ReplayCommit meta, Blob body) {
        try (body) {
            return stageAndCommit(meta, body);
        } catch (IOException e) {
            throw new UncheckedIOException("reading the commit body for replay " + meta.id() + " failed", e);
        }
    }

    @Override
    public ReplayInfo publishCompacted(ReplayCompaction meta, Blob body) {
        try (body) {
            return stageAndPublish(meta, body);
        } catch (IOException e) {
            throw new UncheckedIOException("reading the compacted body for replay " + meta.id() + " failed", e);
        }
    }

    private ReplayInfo stageAndCommit(ReplayCommit meta, Blob body) throws IOException {
        requireIdAndKey(meta.id(), meta.idempotencyKey(), body);
        var expectedDigest = digest(meta.contentDigest(), body);
        var creating = meta.expectedRevision() == null;
        var revision = creating ? 0 : meta.expectedRevision();
        var segmentIndex = meta.segmentIndex();
        if (segmentIndex != null && segmentIndex < 0)
            throw body.refuse(400, "negative segment index: " + segmentIndex);
        if (segmentIndex == null && !meta.finished())
            throw body.refuse(400, "a commit with no segment must finish the replay");
        if (meta.finished() && meta.outcome() == ReplayOutcome.UNKNOWN)
            throw body.refuse(400, "unknown replay outcome");

        var preambleLength = requirePreambleLength(meta.preambleLength(), body);
        var total = body.requireLength();
        if (total > MAX_COMMIT_BYTES)
            throw body.refuse(413, "commit is " + total + " bytes, over the limit");
        if (total < preambleLength)
            throw body.refuse(400, "commit is shorter than the preamble it declares");
        var segmentLength = total - preambleLength;
        if (segmentLength > MAX_SEGMENT_BYTES)
            throw body.refuse(413, "segment is " + segmentLength + " bytes, over the limit");
        if (segmentIndex == null && segmentLength != 0)
            throw body.refuse(400, "segment bytes were sent with no segment index");
        if (segmentIndex != null && segmentLength == 0)
            throw body.refuse(400, "segment index " + segmentIndex + " was sent with no segment bytes");

        // The fingerprint is Go's, so the revision goes into it as the entity tag Go hashed.
        var fingerprint = ReplayCompat.fingerprint("PATCH", meta.id(),
            creating ? "" : ReplayCompat.etag(revision), creating ? "*" : "",
            Integer.toString(preambleLength), segmentIndex == null ? "" : Integer.toString(segmentIndex),
            Boolean.toString(meta.finished()), expectedDigest);

        // Before a byte of the body is read, so a retried commit uploads nothing. Go stages first
        // and then answers from the record, which is what orphans the object.
        var replayed = replayed(db.replays.getReplayIdempotency(meta.id(), meta.idempotencyKey()), fingerprint, body);
        if (replayed != null) return replayed;
        // Likewise before the upload: a stale revision is knowable from the row.
        precheck(db.replays.getReplay(meta.id()), creating, revision, segmentIndex, body);

        var bodyDigest = Digest.sha256();
        var preamble = body.read(preambleLength, "preamble");
        bodyDigest.update(preamble);

        String segmentObject = null;
        var referenced = false;
        try {
            byte[] segmentData = null;
            byte[] segmentDigest = null;
            if (segmentIndex != null) {
                var digest = Digest.sha256();
                if (segmentLength <= MAX_INLINE_SEGMENT_BYTES) {
                    segmentData = body.read((int) segmentLength, "segment");
                    digest.update(segmentData);
                    bodyDigest.update(segmentData);
                } else {
                    segmentObject = ReplayCompat.objectKey(meta.id(), "segments/" + segmentIndex);
                    s3.put(segmentObject, Digest.tee(body.stream(), digest, bodyDigest), segmentLength);
                }
                segmentDigest = digest.digest();
            }
            if (!Arrays.equals(bodyDigest.digest(), expectedDigest))
                throw new IpcException(422, "digest_mismatch");

            var segment = segmentIndex == null ? null : new Segment(
                segmentIndex, segmentObject, segmentData, segmentLength, segmentDigest);
            var result = applyCommit(meta, preamble, fingerprint, creating, revision, segment);
            referenced = result.objectReferenced();
            return result.info();
        } finally {
            if (!referenced) discard(segmentObject);
        }
    }

    /// Lock the row, check it still says what staging was told, write. Nothing before this held the
    /// lock that serialises two racing commits.
    private Applied applyCommit(ReplayCommit meta, byte[] preamble, byte[] fingerprint,
                                boolean creating, long revision, @Nullable Segment segment) {
        var state = meta.finished() ? FINISHED : ReplayCompat.RECORDING;
        var outcome = ReplayCompat.outcome(meta.finished(), meta.outcome());
        var preambleDigest = Digest.sha256(preamble);

        return db.txResult(tx -> {
            var row = tx.replays.getReplayForUpdate(meta.id());
            if (row == null) {
                if (!creating)
                    throw new IpcException(412, "no replay " + meta.id() + " at revision " + revision);
                if (segment != null && segment.index() != 0)
                    throw new IpcException(409, "wrong_segment_index");

                var created = tx.replays.createReplayIfAbsent(new ReplaysQueries.CreateReplayIfAbsentParams(
                    meta.id(), state, segment == null ? 0 : 1, preamble, preambleDigest, outcome));
                if (created != null) {
                    // No idempotency check: nothing can have recorded a response for a replay that
                    // did not exist a statement ago.
                    if (segment != null) insertSegment(tx, meta.id(), segment, created.recordingRevision());
                    recordResponse(tx, meta.id(), meta.idempotencyKey(), fingerprint, 201, created);
                    if (meta.finished()) enqueueCompaction(tx, meta.id());
                    return new Applied(info(created), true);
                }

                // Somebody else won the insert. Go answers 500 when that transaction has since
                // rolled back and there is nothing to lock; losing a race is a 412.
                row = tx.replays.getReplayForUpdate(meta.id());
                if (row == null) throw new IpcException(412, "replay " + meta.id() + " was created and withdrawn");
            }

            var replayed = replayed(tx.replays.getReplayIdempotency(meta.id(), meta.idempotencyKey()), fingerprint, null);
            if (replayed != null) return new Applied(replayed, false);

            if (creating || revision != row.version())
                throw new IpcException(412, "replay " + meta.id() + " is at revision " + row.version());
            if (FINISHED.equals(row.state())) throw new IpcException(409, "replay_finished");
            if (segment != null && segment.index() != row.nextSegmentIndex())
                throw new IpcException(409, "wrong_segment_index");

            var updated = tx.replays.updateReplayRecording(new ReplaysQueries.UpdateReplayRecordingParams(
                state, segment == null ? row.nextSegmentIndex() : row.nextSegmentIndex() + 1,
                preamble, preambleDigest, outcome, meta.id()));
            if (updated == null) throw new IpcException(500, "replay " + meta.id() + " vanished under its own lock");

            if (segment != null) insertSegment(tx, meta.id(), segment, updated.recordingRevision());
            recordResponse(tx, meta.id(), meta.idempotencyKey(), fingerprint, 200, updated);
            if (meta.finished()) enqueueCompaction(tx, meta.id());
            return new Applied(info(updated), true);
        });
    }

    private ReplayInfo stageAndPublish(ReplayCompaction meta, Blob body) throws IOException {
        requireIdAndKey(meta.id(), meta.idempotencyKey(), body);
        var expectedDigest = digest(meta.contentDigest(), body);

        var preambleLength = requirePreambleLength(meta.preambleLength(), body);
        var total = body.requireLength();
        if (total > MAX_COMPACTED_BYTES)
            throw body.refuse(413, "compacted replay is " + total + " bytes, over the limit");
        if (total < preambleLength)
            throw body.refuse(400, "compacted replay is shorter than the preamble it declares");

        var fingerprint = ReplayCompat.fingerprint("PUT", meta.id(),
            ReplayCompat.etag(meta.expectedRevision()), "",
            Integer.toString(preambleLength), "", "", expectedDigest);

        var replayed = replayed(db.replays.getReplayIdempotency(meta.id(), meta.idempotencyKey()), fingerprint, body);
        if (replayed != null) return replayed;
        // Go uploads up to 2 GiB before looking at the row at all, so a publication for a replay
        // that is gone, stale or still recording burns the transfer and leaves the object.
        var current = db.replays.getReplay(meta.id());
        if (current == null) throw body.refuse(404, "no replay " + meta.id());
        if (meta.expectedRevision() != current.version())
            throw body.refuse(412,
                "replay " + meta.id() + " is at revision " + current.version());
        if (!FINISHED.equals(current.state())) throw body.refuse(409, "replay_not_finished");

        var bodyDigest = Digest.sha256();
        var preamble = body.read(preambleLength, "preamble");

        // The object is the whole body, preamble included, and the row keeps the preamble too so a
        // reader that only wants the index does not fetch the replay. The digest is fed from the
        // upload, or the preamble would be counted into it twice.
        var object = ReplayCompat.objectKey(meta.id(), COMPACTED);
        var referenced = false;
        try {
            s3.put(object, Digest.tee(
                new SequenceInputStream(new ByteArrayInputStream(preamble), body.stream()), bodyDigest), total);
            if (!Arrays.equals(bodyDigest.digest(), expectedDigest))
                throw new IpcException(422, "digest_mismatch");
            var result = applyPublication(meta, preamble, fingerprint, object, total, expectedDigest);
            referenced = result.objectReferenced();
            return result.info();
        } finally {
            if (!referenced) discard(object);
        }
    }

    /// As [#applyCommit]: staging uploaded a whole replay on the strength of a read nothing held,
    /// so the row is checked again under its lock.
    private Applied applyPublication(ReplayCompaction meta, byte[] preamble, byte[] fingerprint,
                                     String object, long length, byte[] bodyDigest) {
        var preambleDigest = Digest.sha256(preamble);

        return db.txResult(tx -> {
            var row = tx.replays.getReplayForUpdate(meta.id());
            if (row == null) throw new IpcException(404, "no replay " + meta.id());

            var replayed = replayed(tx.replays.getReplayIdempotency(meta.id(), meta.idempotencyKey()), fingerprint, null);
            if (replayed != null) return new Applied(replayed, false);

            if (meta.expectedRevision() != row.version())
                throw new IpcException(412, "replay " + meta.id() + " is at revision " + row.version());
            if (!FINISHED.equals(row.state())) throw new IpcException(409, "replay_not_finished");

            var published = tx.replays.publishReplayCompacted(new ReplaysQueries.PublishReplayCompactedParams(
                preamble, preambleDigest, object, length, bodyDigest, meta.id()));
            if (published == null) throw new IpcException(500, "replay " + meta.id() + " vanished under its own lock");

            recordResponse(tx, meta.id(), meta.idempotencyKey(), fingerprint, 200, published);
            return new Applied(info(published), true);
        });
    }

    /// Inside the commit's own transaction, so the row lands with the commit that made it necessary
    /// or not at all. A retried final commit returns from the idempotency record before it reaches
    /// here, which is right — the original already enqueued.
    private static void enqueueCompaction(ApiDatabase.Tx tx, String id) {
        JobSpec.COMPACT_REPLAY.enqueue(tx.jobs, new CompactReplay(id, "final-commit"));
    }

    private static void insertSegment(ApiDatabase.Tx tx, String id, Segment segment, long commitRevision) {
        tx.replays.createReplaySegment(new ReplaysQueries.CreateReplaySegmentParams(
            id, segment.index(), segment.object(), segment.data(), segment.length(),
            segment.digest(), commitRevision));
    }

    private static void recordResponse(ApiDatabase.Tx tx, String id, String key, byte[] fingerprint, int status, Replays row) {
        tx.replays.createReplayIdempotency(new ReplaysQueries.CreateReplayIdempotencyParams(
            id, key, fingerprint, status, ReplayCompat.etag(row.version()), ReplayCompat.recorded(row).toString()));
    }

    /// The recorded response for a request already answered, or null. `body` is given only by the
    /// caller that has not read it yet, since nothing else will.
    private @Nullable ReplayInfo replayed(@Nullable ReplayIdempotency record, byte[] fingerprint, @Nullable Blob body) {
        if (record == null) return null;
        if (body != null) body.drain();
        if (!Arrays.equals(record.requestFingerprint(), fingerprint))
            throw new IpcException(409, "idempotency_key_conflict");

        var recorded = JsonParser.parseString(record.responseMetadata()).getAsJsonObject();
        // A record Go wrote has only its three fields, so the rest come off the row.
        var row = recorded.has("preambleLength") ? null : db.replays.getReplay(record.replayId());
        return new ReplayInfo(record.replayId(), ReplayCompat.revision(record.responseEtag()),
            ReplayCompat.state(string(recorded, "state")),
            ReplayCompat.representation(string(recorded, "representation")),
            recorded.has("nextSegmentIndex") ? recorded.get("nextSegmentIndex").getAsInt() : null,
            recorded.has("preambleLength") ? recorded.get("preambleLength").getAsInt()
                : row == null ? 0 : row.currentPreamble().length,
            ReplayCompat.outcome(recorded.has("outcome") ? recorded.get("outcome").getAsString()
                : row == null ? null : row.outcome()),
            recorded.has("updatedAt") ? recorded.get("updatedAt").getAsLong()
                : row == null ? 0 : row.updatedAt().toEpochMilli());
    }

    private static @Nullable String string(JsonObject object, String name) {
        return object.has(name) ? object.get(name).getAsString() : null;
    }

    /// Read before anything is uploaded. A row that says the write will fail says so now; one that
    /// says it will succeed proves nothing, since the transaction checks again under the lock.
    private void precheck(@Nullable Replays row, boolean creating, long revision,
                          @Nullable Integer segmentIndex, Blob body) {
        if (row == null) {
            if (!creating)
                throw body.refuse(412, "no replay at revision " + revision);
            if (segmentIndex != null && segmentIndex != 0)
                throw body.refuse(409, "wrong_segment_index");
            return;
        }
        if (creating || revision != row.version())
            throw body.refuse(412,
                "replay " + row.id() + " is at revision " + row.version());
        if (FINISHED.equals(row.state())) throw body.refuse(409, "replay_finished");
        if (segmentIndex != null && segmentIndex != row.nextSegmentIndex())
            throw body.refuse(409, "wrong_segment_index");
    }

    private Blob object(@Nullable String key, @Nullable Long start, @Nullable Long endInclusive) {
        if (key == null) throw new IpcException(500, "replay object reference is missing");
        try {
            return start == null ? s3.get(key) : s3.getRange(key, start, endInclusive);
        } catch (S3Client.NotFoundError e) {
            // A row pointing at nothing is corruption; a 404 would read as never recorded.
            logger.error("replay object {} is referenced but is not in the bucket", key);
            throw new IpcException(500, "stored replay object " + key + " is missing");
        }
    }

    private void discard(@Nullable String key) {
        if (key == null) return;
        try {
            s3.delete(key);
        } catch (RuntimeException e) {
            // The write is failing either way; an orphan is the lesser problem.
            logger.warn("could not remove the staged replay object {}", key, e);
        }
    }

    private void requireIdAndKey(String id, String key, Blob body) {
        if (id.isEmpty() || id.length() > MAX_ID_LENGTH)
            throw body.refuse(400, "not a replay id");
        if (key.isEmpty() || key.length() > MAX_ID_LENGTH)
            throw body.refuse(400, "not an idempotency key");
    }

    private int requirePreambleLength(int declared, Blob body) {
        if (declared < 0) throw body.refuse(400, "negative preamble length: " + declared);
        if (declared > MAX_PREAMBLE_BYTES)
            throw body.refuse(413, "preamble is " + declared + " bytes, over the limit");
        return declared;
    }

    private byte[] digest(String contentDigest, Blob body) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(contentDigest.strip());
        } catch (IllegalArgumentException e) {
            throw body.refuse(400, "content digest is not base64");
        }
        if (decoded.length != 32) throw body.refuse(400, "content digest is not a sha-256");
        return decoded;
    }

    /// Whether the object staging uploaded is now pointed at by a row decides whether staging
    /// deletes it: an idempotency hit under the lock succeeds without referencing this upload.
    private record Applied(ReplayInfo info, boolean objectReferenced) {
    }

    private record Segment(int index, @Nullable String object, @Nullable byte[] data, long length, byte[] digest) {
    }
}

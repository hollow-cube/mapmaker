package net.hollowcube.apiworker.jobs;

import dev.hollowcube.replay.data.ReplayPreamble;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.job.JobSpec;
import net.hollowcube.apiserver.job.SampleReplayTranscode;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.ipc.replay.ReplayRepresentation;
import net.hollowcube.ipc.replay.ReplayService;
import net.hollowcube.ipc.replay.ReplayState;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/// Transcodes a sample of the replays a rewrite would take in memory, logging the raw and compacted
/// size change.
/// Writes nothing.
public final class SampleReplayTranscodeRunner implements JobRunner<SampleReplayTranscode> {

    private static final Logger logger = LoggerFactory.getLogger(SampleReplayTranscodeRunner.class);

    private final ApiDatabase db;
    private final ReplayService replays;
    private final Duration recordingIdle;

    public SampleReplayTranscodeRunner(
        ApiDatabase db,
        ReplayService replays,
        Duration recordingIdle
    ) {
        this.db = db;
        this.replays = replays;
        this.recordingIdle = recordingIdle;
    }

    @Override
    public void run(@Nullable SampleReplayTranscode data) throws InterruptedException {
        if (data == null || data.count() <= 0)
            throw new IllegalArgumentException(
                "sample-replay-transcode needs data: {\"count\": 500}"
            );

        var job = JobSpec.TRANSCODE_REPLAY.name();
        var after = data.after() != null ? data.after() : UUID.randomUUID().toString();
        var idleBefore = Instant.now().minus(recordingIdle);
        var ids = new ArrayList<>(
            db.replays.listLegacyFormatReplays(idleBefore, after, job, data.count())
        );
        // A random start near the end of the ids would come up short.
        if (data.after() == null && ids.size() < data.count())
            for (var id : db.replays.listLegacyFormatReplays(
                idleBefore,
                "",
                job,
                data.count() - ids.size()
            ))
                if (!ids.contains(id)) ids.add(id);

        var total = new Sizes();
        var segmented = 0;
        var recording = 0;
        var failed = 0;
        for (var id : ids) {
            if (Thread.interrupted()) throw new InterruptedException();
            try {
                var sizes = sample(id);
                if (sizes == null) continue;
                total.add(sizes);
                if (sizes.segmented) segmented++;
                if (sizes.recording) recording++;
                logger.info("sample {}: {}", id, sizes);
            } catch (Exception e) {
                failed++;
                logger.warn("sample {}: failed to transcode", id, e);
            }
        }

        logger.info(
            "sampled {} replays ({} segmented, {} recording, {} failed): {}",
            total.replays,
            segmented,
            recording,
            failed,
            total
        );
    }

    private @Nullable Sizes sample(String id) throws Exception {
        var info = replays.getReplay(id);
        if (info == null) return null;
        final byte[] preamble;
        try (var blob = replays.getPreamble(id, info.revision())) {
            preamble = blob.readAllBytes();
        }
        if (!StoredReplay.needsRewrite(preamble)) return null;

        var stored = StoredReplay.read(replays, info, preamble);
        var untranscoded = stored.untranscoded();
        var transcoded = stored.transcode();

        var sizes = new Sizes();
        sizes.replays = 1;
        sizes.segmented = info.representation() == ReplayRepresentation.SEGMENTED;
        sizes.recording = info.state() == ReplayState.RECORDING;
        sizes.stored = stored.storedBytes();
        sizes.rawBefore = stored.rawBytes();
        sizes.rawAfter = StoredReplay.rawBytes(ReplayPreamble.index(transcoded.data()));
        sizes.compactedBefore = untranscoded.length;
        sizes.compactedAfter = transcoded.data().length;
        return sizes;
    }

    private static final class Sizes {
        int replays;
        boolean segmented;
        boolean recording;
        long stored;
        long rawBefore;
        long rawAfter;
        long compactedBefore;
        long compactedAfter;

        void add(Sizes other) {
            replays += other.replays;
            stored += other.stored;
            rawBefore += other.rawBefore;
            rawAfter += other.rawAfter;
            compactedBefore += other.compactedBefore;
            compactedAfter += other.compactedAfter;
        }

        @Override
        public String toString() {
            return "stored %d, raw %d -> %d (%s), compacted %d -> %d (%s)".formatted(
                stored, rawBefore, rawAfter, growth(rawBefore, rawAfter),
                compactedBefore, compactedAfter, growth(compactedBefore, compactedAfter)
            );
        }

        private static String growth(long before, long after) {
            if (before == 0) return "n/a";
            return "%+.2f%%".formatted(100.0 * (after - before) / before);
        }
    }
}

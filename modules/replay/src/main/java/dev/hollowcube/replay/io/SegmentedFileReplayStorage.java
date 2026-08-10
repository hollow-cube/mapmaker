package dev.hollowcube.replay.io;

import dev.hollowcube.replay.data.ReplayPreamble;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Filesystem-backed segmented replay storage.
///
/// There is no revision token, because a local recording has no concurrent appenders to guard
/// against.
public final class SegmentedFileReplayStorage implements SegmentedReplayStorage {
    private final Path root;

    public SegmentedFileReplayStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public @Nullable SegmentedReplay load(String replayId) {
        var replayRoot = resolve(replayId);
        var preamblePath = replayRoot.resolve(SegmentedFileReplayWriter.PREAMBLE_NAME);
        if (Files.notExists(preamblePath)) return null;

        try {
            var preamble = ReplayPreamble.read(Files.readAllBytes(preamblePath));
            validateReferencedSegments(replayRoot, preamble);
            var finished = Files.exists(replayRoot.resolve(SegmentedFileReplayWriter.FINISHED_NAME));
            return new SegmentedReplay(preamble, null, finished);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to load replay " + replayId, e);
        }
    }

    @Override
    public SegmentedReplayWriter writer(String replayId, @Nullable SegmentedReplay base) {
        return new SegmentedFileReplayWriter(resolve(replayId));
    }

    private Path resolve(String replayId) {
        var idPath = Path.of(replayId);
        if (idPath.isAbsolute() || idPath.getNameCount() != 1
            || replayId.equals(".") || replayId.equals("..")) {
            throw new IllegalArgumentException("invalid replay ID");
        }
        return root.resolve(idPath);
    }

    private static void validateReferencedSegments(Path replayRoot, ReplayPreamble preamble) throws IOException {
        for (var chunk : preamble.index()) {
            int segmentIndex = ReplayPreamble.segmentIndex(chunk);
            long requiredLength = Math.addExact(
                ReplayPreamble.segmentOffset(chunk),
                chunk.compressedLength()
            );
            var segmentPath = replayRoot.resolve(SegmentedFileReplayWriter.segmentName(segmentIndex));
            if (Files.notExists(segmentPath) || Files.size(segmentPath) < requiredLength) {
                throw new IllegalArgumentException(
                    "replay preamble references missing or truncated segment " + segmentIndex
                );
            }
        }
    }
}

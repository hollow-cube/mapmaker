package dev.hollowcube.replay.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntFunction;

/// Reads the segments of a local recording, for compacting it.
public final class SegmentedFileReplaySource implements IntFunction<byte[]> {
    private final Path root;

    public SegmentedFileReplaySource(Path root) {
        this.root = root;
    }

    @Override
    public byte[] apply(int segmentIndex) {
        try {
            return Files.readAllBytes(root.resolve(SegmentedFileReplayWriter.segmentName(segmentIndex)));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read replay segment " + segmentIndex, e);
        }
    }
}

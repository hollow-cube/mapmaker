package dev.hollowcube.replay.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class SegmentedFileReplayWriter implements SegmentedReplayWriter {
    static final String PREAMBLE_NAME = "preamble.dat";
    static final String FINISHED_NAME = "finished";
    private static final String SEGMENT_FORMAT = "segment-%03d.dat";

    private final Path root;

    public SegmentedFileReplayWriter(Path root) {
        this.root = root;
    }

    @Override
    public void commit(SegmentedReplayCommit commit) {
        try {
            Files.createDirectories(root);

            // Segment first, preamble second. A crash between the two leaves an unreferenced
            // segment, which the next append overwrites; the reverse would leave a committed
            // preamble pointing at bytes that never landed.
            if (commit.hasSegment()) writeSegment(commit.segmentIndex(), commit.segment());
            writePreamble(commit.preamble());
            if (commit.finished()) Files.write(root.resolve(FINISHED_NAME), new byte[0]);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        // Noop
    }

    private void writePreamble(byte[] preamble) throws IOException {
        var preamblePath = root.resolve(PREAMBLE_NAME);
        var temporaryPath = Files.createTempFile(root, ".preamble-", ".tmp");
        try {
            Files.write(temporaryPath, preamble, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(
                temporaryPath,
                preamblePath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    private void writeSegment(int index, byte[] segment) throws IOException {
        Files.write(
            root.resolve(segmentName(index)),
            segment,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    static String segmentName(int index) {
        return String.format(SEGMENT_FORMAT, index);
    }
}

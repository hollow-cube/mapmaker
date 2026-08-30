package net.hollowcube.anticheat.log;

import java.util.List;
import java.util.function.Consumer;

/// The frames of a capture, in order, however the caller happens to hold them — a spool file being
/// read back, the ring, or a list in a test.
///
/// Push rather than pull because the spool is read with a stream that wants to own its own loop and
/// close itself when it ends.
@FunctionalInterface
public interface FrameSource {

    FrameSource EMPTY = sink -> {
    };

    void forEach(Consumer<Frame> sink);

    static FrameSource of(List<Frame> frames) {
        return frames::forEach;
    }
}

package dev.hollowcube.replay;

import dev.hollowcube.replay.data.ChunkIndex;
import dev.hollowcube.replay.data.ReplayHeader;
import dev.hollowcube.replay.event.ReplayEvent;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

/// ReplayRecorder represents an active recording in progress.
///
/// It may be created from scratch, or resumed from a previous partial recording.
final class ReplayRecorderImpl implements ReplayRecorder {

    private final ReplayHeader header = null;
    private final CompoundBinaryTag.Builder metadata = null;
    private final List<ChunkIndex> index = null;

    @Override
    public void advance() {

    }

    @Override
    public void submit(ReplayEvent event) {

    }
}

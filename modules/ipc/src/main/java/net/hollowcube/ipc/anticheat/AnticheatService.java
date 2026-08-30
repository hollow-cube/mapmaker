package net.hollowcube.ipc.anticheat;

import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.util.Ipc;

import java.util.List;

/// The anticheat capture traces: the blobs on the shared volume and the index over them.
///
/// A service like any other, blobs and all — the alternative was a hand-written handler beside the
/// generated ones, and a replay is the same shape of thing, so the wire may as well carry bytes.
@Ipc
public interface AnticheatService {

    /// Stores one trace, filing the row after the blob is on the volume so that an interrupted
    /// call leaves a file nothing points at rather than a row pointing at nothing.
    ///
    /// Refuses a body that does not open with the container's magic, or one longer than the store
    /// accepts: half a trace is not evidence, and the proxy that shipped it still has its copy.
    PutResult putTrace(TraceMeta meta, Blob body);

    /// The trace's bytes, exactly as they were stored. 404 for one nothing stored, and for one
    /// whose blob went missing under its row.
    Blob getTrace(String id);

    /// Every trace of one capture, oldest first. Unpaged: a capture is a handful of traces,
    /// since only a server switch or a ring flush starts a new one.
    List<TraceRow> listTraces(String captureId);
}

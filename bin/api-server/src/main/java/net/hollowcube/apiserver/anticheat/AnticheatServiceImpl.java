package net.hollowcube.apiserver.anticheat;

import net.hollowcube.apiserver.db.AnticheatQueries;
import net.hollowcube.apiserver.db.AnticheatTraces;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.anticheat.AnticheatService;
import net.hollowcube.ipc.anticheat.PutResult;
import net.hollowcube.ipc.anticheat.TraceMeta;
import net.hollowcube.ipc.anticheat.TraceRow;
import net.hollowcube.ipc.util.IpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static net.hollowcube.ipc.util.IpcArgs.*;

/// The capture traces: their blobs on the shared volume, their index in Postgres.
///
/// The blob is the record and the row is only the way in, which is why the file is renamed into
/// place before the row is written and never the other way round: an interrupted put leaves a file
/// nothing points at, and the retry that follows overwrites it.
///
/// There is no authentication, for the same reason nothing else here has any: this listens on the
/// internal network only. The id is still held to a path-component shape, because that is a
/// property of the filesystem rather than of who is calling.
public final class AnticheatServiceImpl implements AnticheatService {

    /// The check constraint's spelling of a capture reason. Checked here so an unknown one is a bad
    /// request naming the field rather than a constraint violation naming the table.
    private static final Set<String> REASONS = Set.of("run", "sample", "flag", "manual");

    private static final Logger logger = LoggerFactory.getLogger(AnticheatServiceImpl.class);

    private final ApiDatabase db;
    private final AnticheatTraceStore store;

    public AnticheatServiceImpl(ApiDatabase db, AnticheatTraceStore store) {
        this.db = db;
        this.store = store;
    }

    @Override
    public PutResult putTrace(TraceMeta meta, Blob body) {
        if (!AnticheatTraceStore.validId(meta.id()))
            throw new IpcException(400, "not a trace id: " + meta.id());
        if (meta.reason() != null && !REASONS.contains(meta.reason()))
            throw new IpcException(400, "no such capture reason: " + meta.reason());

        var startedAt = Instant.ofEpochMilli(meta.startedAt());
        var path = AnticheatTraceStore.pathOf(meta.id(), startedAt);
        AnticheatTraceStore.Result written;
        try {
            written = store.write(path, body.stream());
        } catch (IOException e) {
            throw new UncheckedIOException("storing trace " + meta.id() + " failed", e);
        }

        return switch (written) {
            case AnticheatTraceStore.Result.TooLarge _ ->
                throw new IpcException(413, "trace is longer than the store accepts");
            case AnticheatTraceStore.Result.NotATrace _ ->
                throw new IpcException(400, "body does not open with the HCTR magic");
            case AnticheatTraceStore.Result.Stored stored -> {
                // Read before the upsert only to tell the proxy which of its retries this was; the
                // two racing is a pair of first uploads for one trace, which is nothing.
                var replaced = db.anticheat.getAnticheatTrace(meta.id()) != null;
                db.anticheat.upsertAnticheatTrace(new AnticheatQueries.UpsertAnticheatTraceParams(
                    meta.id(), meta.captureId(), uuid(meta.playerId(), "playerId"), meta.proxyVersion(), meta.proxy(),
                    meta.clientPvn(), meta.reason(), stored.formatVersion(), startedAt, instant(meta.endedAt()),
                    stored.bytes(), path));

                logger.atInfo()
                    .setMessage("stored trace")
                    .addKeyValue("trace", meta.id())
                    .addKeyValue("capture", meta.captureId())
                    .addKeyValue("player", meta.playerId())
                    .addKeyValue("bytes", stored.bytes())
                    .addKeyValue("path", path)
                    .log();

                yield new PutResult(path, stored.bytes(), replaced);
            }
        };
    }

    @Override
    public Blob getTrace(String id) {
        var row = db.anticheat.getAnticheatTrace(id);
        if (row == null) throw new IpcException(404, "no trace " + id);

        var file = store.resolve(row.path());
        if (!Files.isRegularFile(file)) {
            // The row is only written once the blob is in place, so this is one that went missing
            // underneath us — worth a line, since nothing here deletes.
            logger.warn("trace {} is indexed at {} but there is no file there", id, row.path());
            throw new IpcException(404, "no trace " + id);
        }
        try {
            return Blob.of(file);
        } catch (IOException e) {
            throw new UncheckedIOException("reading trace " + id + " failed", e);
        }
    }

    @Override
    public List<TraceRow> listTraces(String captureId) {
        return db.anticheat.listAnticheatTracesByCapture(captureId).stream()
            .map(AnticheatServiceImpl::row)
            .toList();
    }

    private static TraceRow row(AnticheatTraces row) {
        var meta = new TraceMeta(row.id(), row.captureId(), row.playerId().toString(), row.proxyVersion(),
            row.proxy(), row.clientPvn(), row.reason(), row.startedAt().toEpochMilli(), millis(row.endedAt()),
            row.formatVersion());
        return new TraceRow(meta, row.bytes(), row.path(), row.pinned(), millis(row.expiresAt()),
            row.createdAt().toEpochMilli());
    }
}

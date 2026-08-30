package net.hollowcube.ipc.anticheat;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

/// One stored trace, as the index holds it: what the proxy said about it and what the store knows
/// on top of that.
///
/// @param bytes     the blob's length, as it landed
/// @param path      where it landed, relative to the store root
/// @param pinned    whether a retention sweep has to leave it alone; nothing sets this yet
/// @param expiresAt when a sweep may take it, in epoch milliseconds, and null while there is no
///                  retention policy at all
/// @param createdAt when the row was filed, in epoch milliseconds
@RuntimeGson
public record TraceRow(
    TraceMeta meta,
    long bytes,
    String path,
    boolean pinned,
    @Nullable Long expiresAt,
    long createdAt
) {
}

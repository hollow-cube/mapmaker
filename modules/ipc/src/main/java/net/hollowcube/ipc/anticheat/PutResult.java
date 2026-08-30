package net.hollowcube.ipc.anticheat;

import net.hollowcube.common.util.RuntimeGson;

/// Where a stored trace landed.
///
/// @param replaced whether a trace of this id was already there. The proxy retries a ship for up
///                 to ten minutes and a retry ships a longer body under the same id, so this is the
///                 caller's way of telling a first upload from one of its own repeats.
@RuntimeGson
public record PutResult(String path, long bytes, boolean replaced) {
}

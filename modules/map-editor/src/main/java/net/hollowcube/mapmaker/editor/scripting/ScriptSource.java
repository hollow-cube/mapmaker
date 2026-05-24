package net.hollowcube.mapmaker.editor.scripting;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

/// Backing store for script files used by the editing/testing flow. The
/// [DynamicModuleLoader] never talks to a backend directly: it goes through
/// this interface so production (HTTP via [MapClientScriptSource]) and tests
/// (in-memory) plug in symmetrically.
///
/// Implementations are called off the world thread, can block on IO, and are
/// not expected to be thread-safe beyond what their backing store guarantees.
public interface ScriptSource {

    /// All file paths currently in the source store. Paths are returned in the
    /// canonical form the loader will use to read them (eg `/lib/util.luau`).
    @Blocking
    Iterable<String> listFiles();

    /// File contents as utf-8 bytes, or {@code null} if the path is no longer
    /// present. Paths are values previously returned by [#listFiles] or passed
    /// to [ReloadingScriptSession#notifyFilesChanged].
    @Blocking
    byte @Nullable [] readFile(String path);
}

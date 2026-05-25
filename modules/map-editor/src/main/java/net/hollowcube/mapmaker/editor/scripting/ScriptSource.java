package net.hollowcube.mapmaker.editor.scripting;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

/// Wrapper basically around a filesystem. used for loading from
/// * filesystem (inline hub builder)
/// * zip files (script bundles)
/// * in-memory (for testing)
public interface ScriptSource {

    @Blocking
    Iterable<String> listFiles();

    @Blocking
    byte @Nullable [] readFile(String path);
}

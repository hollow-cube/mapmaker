package net.hollowcube.mapmaker.editor.scripting;

import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryScriptSource implements ScriptSource {
    private final Map<String, byte[]> files = new LinkedHashMap<>();

    /// Add or replace a file. Path is canonical (eg `/main.luau`).
    public InMemoryScriptSource put(String path, String source) {
        files.put(normalize(path), source.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /// Remove a file from the source store. The next reload sees it absent.
    public void remove(String path) {
        files.remove(normalize(path));
    }

    @Override
    public Iterable<String> listFiles() {
        return List.copyOf(files.keySet());
    }

    @Override
    public byte @Nullable [] readFile(String path) {
        return files.get(normalize(path));
    }

    private static String normalize(String path) {
        var p = path.replace('\\', '/');
        if (p.startsWith("@")) p = p.substring(1);
        if (!p.startsWith("/")) p = "/" + p;
        return p;
    }
}

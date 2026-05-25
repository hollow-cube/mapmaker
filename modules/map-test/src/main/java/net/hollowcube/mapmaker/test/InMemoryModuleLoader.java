package net.hollowcube.mapmaker.test;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.scripting.require.AbstractModuleLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/// In-memory [AbstractModuleLoader] for runtime tests that don't need the
/// editor reload pipeline. Compiles sources on read using [TestCompilers#EDITOR]
/// so behaviour matches the production editor compiler.
public final class InMemoryModuleLoader extends AbstractModuleLoader {

    private final LuauCompiler compiler;
    private final Map<String, String> sources = new HashMap<>();

    public InMemoryModuleLoader() {
        this(TestCompilers.EDITOR);
    }

    public InMemoryModuleLoader(LuauCompiler compiler) {
        this.compiler = compiler;
    }

    /// Add or replace a file. Path is canonical (eg `/main.luau`).
    public InMemoryModuleLoader put(String path, String source) {
        sources.put(normalize(path), source);
        return this;
    }

    @Override
    protected boolean isFile(String path) {
        return sources.containsKey(normalize(path));
    }

    @Override
    protected byte[] readAndParseFile(String loadName) {
        var src = Objects.requireNonNull(sources.get(normalize(loadName)), loadName);
        try {
            return compiler.compile(src);
        } catch (LuauCompileException e) {
            throw new RuntimeException(loadName, e);
        }
    }

    private static String normalize(String path) {
        var p = path.replace('\\', '/');
        if (p.startsWith("@")) p = p.substring(1);
        if (!p.startsWith("/")) p = "/" + p;
        return p;
    }
}

package net.hollowcube.mapmaker.map.script.loader;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.local.proj.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LocalScriptLoader implements MapScriptLoader {
    private static final LuauCompiler COMPILER = LuauCompiler.DEFAULT;

    private final Path projectRoot;

    private final Map<String, Map.Entry<Integer, byte[]>> bytecodeCache = new HashMap<>();

    public LocalScriptLoader(@NotNull Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    @Override
    public @NotNull ScriptManifest getManifest() {
        final Project project = Project.read(projectRoot);
        return new ScriptManifest(project.scripts().stream()
                .map(script -> new ScriptManifest.Script(script.path(), Path.of(script.path()).getFileName().toString(), script.type()))
                .toList());
    }

    @Override
    public byte @NotNull [] getScriptBytecode(@NotNull String id) {
        try {
            final Path scriptPath = projectRoot.resolve(id);
            if (!Files.exists(scriptPath))
                throw new IllegalArgumentException("Script not found: " + id);
            byte[] source = Files.readAllBytes(scriptPath);
            return COMPILER.compile(source);
        } catch (IOException | LuauCompileException e) {
            throw new RuntimeException(e);
        }
    }
}

package net.hollowcube.mapmaker.scripting.require;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

// uses @/path as chunknames
public class ResourceRequireResolver extends AbstractModuleLoader {
    private final LuauCompiler compiler;
    private final URI base;

    public ResourceRequireResolver(LuauCompiler compiler, URI base) {
        this.compiler = compiler;
        this.base = base;
    }

    @Override
    protected boolean isFile(String path) {
        // filesystem read
        try {
            // erm
            URI.create(base + path).toURL().openStream().close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected byte[] readAndParseFile(String loadName) throws IOException, LuauCompileException {
        try (var is = URI.create(base + loadName).toURL().openStream()) {
            var contents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return compiler.compile(contents);
        }
    }
    
}

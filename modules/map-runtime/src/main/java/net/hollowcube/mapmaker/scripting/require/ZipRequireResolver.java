package net.hollowcube.mapmaker.scripting.require;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// uses @/path as chunknames
public class ZipRequireResolver extends AbstractModuleLoader {
    private static final Logger logger = LoggerFactory.getLogger(ZipRequireResolver.class);

    private final Map<String, byte[]> vfs;

    public ZipRequireResolver(LuauCompiler compiler, URI zip) throws IOException, LuauCompileException {
        this.vfs = readAndCompileEntries(compiler, zip);
    }

    @Deprecated
    public Map<String, byte[]> getVfsThisIsBadPleaseFix() {
        return vfs;
    }

    @Override
    protected boolean isFile(String path) {
        return vfs.containsKey(path);
    }

    @Override
    protected byte[] readAndParseFile(String loadName) {
        return Objects.requireNonNull(vfs.get(loadName), loadName);
    }

    private static Map<String, byte[]> readAndCompileEntries(LuauCompiler compiler, URI zip) throws IOException, LuauCompileException {
        var vfs = new HashMap<String, byte[]>();
        try (var is = zip.toURL().openStream()) {
            if (is == null) throw new IOException("Could not open zip stream for URI: " + zip);

            var zis = new ZipInputStream(is);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                if (!entry.getName().endsWith(".luau"))
                    continue;

                var path = normalizePath("/" + entry.getName());
                var content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                vfs.put(path, compiler.compile(content));
                logger.info("compiled script {} ({} bytes)", path, content.length());
            }
        }
        return Map.copyOf(vfs);
    }
}

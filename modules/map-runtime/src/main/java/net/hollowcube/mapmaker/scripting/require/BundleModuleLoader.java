package net.hollowcube.mapmaker.scripting.require;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// uses @/path as chunknames
public class BundleModuleLoader extends AbstractModuleLoader {
    private final Map<String, byte[]> vfs;

    public BundleModuleLoader(URI zip) throws IOException {
        this.vfs = readEntries(zip);
    }

    @Override
    protected boolean isFile(String path) {
        return vfs.containsKey(path);
    }

    @Override
    protected byte[] readAndParseFile(String loadName) {
        return Objects.requireNonNull(vfs.get(loadName), loadName);
    }

    private static Map<String, byte[]> readEntries(URI zip) throws IOException {
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
                vfs.put(path, zis.readAllBytes());
            }
        }
        return Map.copyOf(vfs);
    }
}

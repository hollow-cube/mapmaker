package net.hollowcube.mapmaker.scripting.require;

import net.hollowcube.mapmaker.scripting.BundleMetadata;
import net.hollowcube.mapmaker.util.AbstractHttpService;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BundleModuleLoader extends AbstractModuleLoader {
    private static final String METADATA_ENTRY = "metadata.json";
    private static final String SCRIPTS_PREFIX = "scripts/";

    private final Map<String, byte[]> vfs;

    public BundleModuleLoader(URI zip) throws IOException {
        this.vfs = readBundle(zip);
    }

    @Override
    protected boolean isFile(String path) {
        return vfs.containsKey(path);
    }

    @Override
    protected byte[] readAndParseFile(String loadName) {
        return Objects.requireNonNull(vfs.get(loadName), loadName);
    }

    /// Single-pass zip walk: collects bytecode under `scripts/` and grabs the metadata.json
    /// payload, then validates after the loop so a missing/mismatched metadata fails the
    /// constructor before any script can run.
    private static Map<String, byte[]> readBundle(URI zip) throws IOException {
        var vfs = new HashMap<String, byte[]>();
        byte[] metadataBytes = null;

        try (var is = zip.toURL().openStream();
             var zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                var name = entry.getName();

                if (name.equals(METADATA_ENTRY)) {
                    metadataBytes = zis.readAllBytes();
                    continue;
                }
                if (!name.startsWith(SCRIPTS_PREFIX) || !name.endsWith(".luau")) continue;

                // Strip the `scripts/` prefix; normalize to a chunk-name path with a leading `/`.
                var path = normalizePath("/" + name.substring(SCRIPTS_PREFIX.length()));
                vfs.put(path, zis.readAllBytes());
            }
        }

        if (metadataBytes == null)
            throw new IOException("bundle zip is missing required `metadata.json` entry: " + zip);
        validate(zip, AbstractHttpService.GSON.fromJson(new String(metadataBytes, StandardCharsets.UTF_8), BundleMetadata.class));

        return Map.copyOf(vfs);
    }

    private static void validate(URI zip, BundleMetadata md) throws IOException {
        if (md.bundleFormatVersion() != BundleMetadata.LATEST_VERSION) {
            var msg = "bundle %s has format version %d but this runtime expects %d."
                .formatted(zip, md.bundleFormatVersion(), BundleMetadata.LATEST_VERSION);
            throw new IOException(msg);
        }

        if (md.luauBytecodeVersion() != BundleMetadata.LUAU_BYTECODE_VERSION) {
            var msg = "bundle %s was built with luau bytecode version %d but this runtime supports %d."
                .formatted(zip, md.luauBytecodeVersion(), BundleMetadata.LUAU_BYTECODE_VERSION);
            throw new IOException(msg);
        }
    }
}

package net.hollowcube.mapmaker.bundle;

import net.hollowcube.luau.compiler.LuauCompileException;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.mapmaker.editor.scripting.ScriptSource;
import net.hollowcube.mapmaker.scripting.BundleMetadata;
import net.hollowcube.mapmaker.util.AbstractHttpService;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BundleAssembler {

    private final LuauCompiler compiler;

    public BundleAssembler(LuauCompiler compiler) {
        this.compiler = compiler;
    }

    public BundleAssembler() {
        this(BundleCompiler.PROD);
    }

    public void assemble(ScriptSource source, Path outputZip) throws IOException {
        Files.createDirectories(outputZip.toAbsolutePath().getParent());

        int observedBytecodeVersion = -1;

        try (OutputStream raw = Files.newOutputStream(outputZip);
             OutputStream buffered = new BufferedOutputStream(raw);
             ZipOutputStream zip = new ZipOutputStream(buffered)) {

            for (String path : source.listFiles()) {
                if (!path.endsWith(".luau")) continue;

                byte[] sourceBytes = source.readFile(path);
                if (sourceBytes == null) throw new IOException(
                    "ScriptSource.listFiles() returned `" + path + "` but readFile() returned null"
                    + " - source store changed mid-bundle");

                byte[] bytecode;
                try {
                    bytecode = compiler.compile(sourceBytes);
                } catch (LuauCompileException e) {
                    throw new IOException("failed to compile " + path + ": " + e.getMessage(), e);
                }

                int version = Byte.toUnsignedInt(bytecode[0]);
                if (observedBytecodeVersion == -1) {
                    observedBytecodeVersion = version;
                } else if (observedBytecodeVersion != version) {
                    throw new IOException(
                        "luau compiler emitted bytecode version " + version + " for " + path
                        + " but " + observedBytecodeVersion + " for earlier files — bundler "
                        + "cannot stamp a single luauBytecodeVersion in metadata");
                }

                var entryName = "scripts" + path;  // path already leads with `/`
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write(bytecode);
                zip.closeEntry();
            }

            int versionForMetadata = observedBytecodeVersion == -1
                ? BundleMetadata.LUAU_BYTECODE_VERSION
                : observedBytecodeVersion;

            var metadata = BundleMetadata.current(versionForMetadata);
            zip.putNextEntry(new ZipEntry("metadata.json"));
            zip.write(AbstractHttpService.GSON_PRETTY.toJson(metadata).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}

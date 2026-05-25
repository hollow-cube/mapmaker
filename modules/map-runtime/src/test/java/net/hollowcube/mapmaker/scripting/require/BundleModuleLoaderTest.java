package net.hollowcube.mapmaker.scripting.require;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.hollowcube.mapmaker.scripting.BundleMetadata;
import net.hollowcube.mapmaker.scripting.bundle.BundleConstants;
import net.hollowcube.mapmaker.scripting.bundle.BundleMetadataJson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the version-validation contract on [BundleModuleLoader]:
///
///  - `bundleFormatVersion` mismatch → `IOException` (refuse; no v1 reader handles v2 layout).
///  - `luauBytecodeVersion` mismatch → `IOException` (refuse; Luau crashes on foreign bytecode).
///  - `serverRuntimeVersion` mismatch → loader constructs OK, a warning is logged.
class BundleModuleLoaderTest {

    private ListAppender<ILoggingEvent> logCapture;

    @BeforeEach
    void attachLogCapture() {
        var loaderLogger = (Logger) LoggerFactory.getLogger(BundleModuleLoader.class);
        logCapture = new ListAppender<>();
        logCapture.start();
        loaderLogger.addAppender(logCapture);
    }

    @AfterEach
    void detachLogCapture() {
        var loaderLogger = (Logger) LoggerFactory.getLogger(BundleModuleLoader.class);
        loaderLogger.detachAppender(logCapture);
        logCapture.stop();
    }

    @Test
    void mismatchedBundleFormatVersionIsRefused(@TempDir Path tmp) throws IOException {
        var zip = writeBundle(tmp.resolve("bad-format.zip"), new BundleMetadata(
            BundleConstants.BUNDLE_FORMAT_VERSION + 1,
            BundleConstants.SERVER_RUNTIME_VERSION,
            BundleConstants.LUAU_BYTECODE_VERSION));

        var ex = assertThrows(IOException.class, () -> new BundleModuleLoader(zip.toUri()));
        assertTrue(ex.getMessage().contains("bundleFormatVersion"),
            "error should name the offending field. Got: " + ex.getMessage());
    }

    @Test
    void mismatchedLuauBytecodeVersionIsRefused(@TempDir Path tmp) throws IOException {
        var zip = writeBundle(tmp.resolve("bad-bytecode.zip"), new BundleMetadata(
            BundleConstants.BUNDLE_FORMAT_VERSION,
            BundleConstants.SERVER_RUNTIME_VERSION,
            BundleConstants.LUAU_BYTECODE_VERSION + 1));

        var ex = assertThrows(IOException.class, () -> new BundleModuleLoader(zip.toUri()));
        assertTrue(ex.getMessage().contains("luauBytecodeVersion"),
            "error should name the offending field. Got: " + ex.getMessage());
    }

    @Test
    void mismatchedServerRuntimeVersionWarnsButLoads(@TempDir Path tmp) throws IOException {
        var zip = writeBundle(tmp.resolve("old-runtime.zip"), new BundleMetadata(
            BundleConstants.BUNDLE_FORMAT_VERSION,
            BundleConstants.SERVER_RUNTIME_VERSION + 1,  // bundle is "newer" than runtime
            BundleConstants.LUAU_BYTECODE_VERSION));

        new BundleModuleLoader(zip.toUri());  // must not throw

        boolean warned = logCapture.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                           && e.getFormattedMessage().contains("serverRuntimeVersion"));
        assertTrue(warned,
            "expected a runtime-version mismatch warning. Captured: " + logCapture.list);
    }

    @Test
    void missingMetadataIsRefused(@TempDir Path tmp) throws IOException {
        // A zip that has scripts but no metadata.json — the loader has no way to know what
        // versions to validate against, so reject. Catches legacy raw-zip bundles produced
        // before the metadata addition.
        var zipPath = tmp.resolve("no-meta.zip");
        try (var out = Files.newOutputStream(zipPath);
             var zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("scripts/main.luau"));
            zos.write(new byte[]{1, 2, 3});  // bogus bytes — won't be reached because constructor fails first
            zos.closeEntry();
        }
        var ex = assertThrows(IOException.class, () -> new BundleModuleLoader(zipPath.toUri()));
        assertTrue(ex.getMessage().contains("metadata.json"),
            "error should name the missing file. Got: " + ex.getMessage());
    }

    /// Write a minimal valid-shaped bundle whose `metadata.json` is whatever the caller hands in.
    /// No script entries — the loader still validates metadata even when there are no scripts.
    private static Path writeBundle(Path zipPath, BundleMetadata metadata) throws IOException {
        try (var out = Files.newOutputStream(zipPath);
             var zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("metadata.json"));
            zos.write(BundleMetadataJson.write(metadata).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return zipPath;
    }
}

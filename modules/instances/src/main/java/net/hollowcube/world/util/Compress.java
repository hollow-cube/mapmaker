package net.hollowcube.world.util;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Compress {

    public static @NotNull byte[] pack(@NotNull Path dir) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zipOutputStream = new ZipOutputStream(new ZstdOutputStream(out))) {
            for (var file : Files.list(dir).toList()) {
                zipOutputStream.putNextEntry(new ZipEntry(file.getFileName().toString()));
                zipOutputStream.write(Files.readAllBytes(file));
                zipOutputStream.closeEntry();
            }
        }
        return out.toByteArray();
    }

    public static void unpack(@NotNull Path dir, @NotNull InputStream is) throws IOException {
        Files.createDirectories(dir);

        var zstdInputStream = new ZstdInputStream(is);
        try (var zipInputStream = new ZipInputStream(zstdInputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Files.copy(zipInputStream, dir.resolve(entry.getName()));
            }
        }
    }
}

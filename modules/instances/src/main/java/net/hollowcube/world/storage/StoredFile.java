package net.hollowcube.world.storage;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;

public record StoredFile(@NotNull InputStream data, long size, @NotNull Map<String, String> metadata) {
}

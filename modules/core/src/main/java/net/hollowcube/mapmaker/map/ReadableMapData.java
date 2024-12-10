package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.nio.channels.ReadableByteChannel;

public record ReadableMapData(@NotNull ReadableByteChannel data, long length) {
}

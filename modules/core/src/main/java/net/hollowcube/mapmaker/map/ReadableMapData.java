package net.hollowcube.mapmaker.map;

import java.nio.channels.ReadableByteChannel;

public record ReadableMapData(ReadableByteChannel data, long length) {
}

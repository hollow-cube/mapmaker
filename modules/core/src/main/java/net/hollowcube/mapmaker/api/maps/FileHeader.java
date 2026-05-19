package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record FileHeader(
    String path,
    String contentType,
    long size,
    String hash
) {
}

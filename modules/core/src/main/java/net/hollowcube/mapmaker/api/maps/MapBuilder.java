package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;

@RuntimeGson
public record MapBuilder(String id, Instant createdAt, boolean pending) {
}

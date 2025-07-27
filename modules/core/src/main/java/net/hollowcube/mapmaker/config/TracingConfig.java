package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record TracingConfig(String otlpEndpoint, boolean noop) {
}

package net.hollowcube.mapmaker.config;

public record TracingConfig(String otlpEndpoint, boolean noop) {
}

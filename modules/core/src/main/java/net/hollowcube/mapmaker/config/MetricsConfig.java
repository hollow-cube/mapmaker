package net.hollowcube.mapmaker.config;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record MetricsConfig(
        String password,
        String pyroscopeEndpoint
) {
}

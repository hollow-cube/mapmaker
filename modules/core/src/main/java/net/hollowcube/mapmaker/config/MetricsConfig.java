package net.hollowcube.mapmaker.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record MetricsConfig(
        String password,
        @Setting("pyroscope_endpoint")
        String pyroscopeEndpoint
) {
}

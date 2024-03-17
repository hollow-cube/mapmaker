package net.hollowcube.mapmaker.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record MetricsConfig(String password) {
}

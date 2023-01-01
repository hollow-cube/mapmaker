package net.hollowcube.mapmaker.dev.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record HttpConf(
        String host,
        int port
) {
}

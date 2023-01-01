package net.hollowcube.mapmaker.dev.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record MinestomConf(
        String host,
        int port
) {
}

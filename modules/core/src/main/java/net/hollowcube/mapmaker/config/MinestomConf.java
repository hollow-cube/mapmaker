package net.hollowcube.mapmaker.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record MinestomConf(
        String host,
        int port
) {
}

package net.hollowcube.mapmaker.dev.http;

import net.hollowcube.common.config.ConfigPath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigPath("http")
@ConfigSerializable
public record HttpConfig(
        String host,
        int port
) {
}

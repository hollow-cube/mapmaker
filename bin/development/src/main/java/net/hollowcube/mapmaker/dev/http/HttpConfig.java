package net.hollowcube.mapmaker.dev.http;

import net.hollowcube.mapmaker.config.ConfigPath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigPath("http")
@ConfigSerializable
public record HttpConfig(
        String host,
        int port
) {
}

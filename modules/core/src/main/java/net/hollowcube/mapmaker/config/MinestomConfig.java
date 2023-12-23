package net.hollowcube.mapmaker.config;

import net.hollowcube.common.config.ConfigPath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigPath("minestom")
@ConfigSerializable
public record MinestomConfig(
        String host,
        int port
) {
}

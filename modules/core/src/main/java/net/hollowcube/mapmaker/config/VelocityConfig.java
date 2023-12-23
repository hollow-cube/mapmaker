package net.hollowcube.mapmaker.config;

import net.hollowcube.common.config.ConfigPath;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigPath("velocity")
@ConfigSerializable
public record VelocityConfig(
        String secret
) {
}

package net.hollowcube.mapmaker.dev.config;

import net.hollowcube.common.config.S3Config;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record S3Conf(
        String uri
) implements S3Config {
}

package net.hollowcube.mapmaker.dev.config;

import net.hollowcube.common.config.S3Config;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record S3Conf(
        String address,
        String region,
        @Setting("accessKey") String accessKey,
        @Setting("secretKey") String secretKey
) implements S3Config {
}

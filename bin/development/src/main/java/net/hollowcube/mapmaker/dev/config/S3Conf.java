package net.hollowcube.mapmaker.dev.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record S3Conf(
        String address,
        String region,
        @Setting("accessKey") String accessKey,
        @Setting("secretKey") String secretKey
) {
}

package net.hollowcube.mapmaker.dev.config;

import net.hollowcube.common.config.SpiceDBConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record SpiceDBConf(
        String address,
        @Setting("secretKey") String secretKey,
        boolean tls
) implements SpiceDBConfig {
}

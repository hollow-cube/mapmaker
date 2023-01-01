package net.hollowcube.mapmaker.dev.config;

import net.hollowcube.common.config.MongoConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public record MongoConf(
        String uri,
        String database,
        @Setting("useTransactions") boolean useTransactions
) implements MongoConfig {
}

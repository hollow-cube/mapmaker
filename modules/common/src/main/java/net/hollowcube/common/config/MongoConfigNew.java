package net.hollowcube.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigPath("mongo")
@ConfigSerializable
public record MongoConfigNew(
        String uri,
        String database,
        boolean useTransactions
) implements MongoConfig {

}

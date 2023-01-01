package net.hollowcube.mapmaker.dev.config;

import net.hollowcube.common.config.MongoConfig;

public record MongoConf(
        String uri,
        String database,
        boolean useTransactions
) implements MongoConfig {
}

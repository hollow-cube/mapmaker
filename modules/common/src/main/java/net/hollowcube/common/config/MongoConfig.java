package net.hollowcube.common.config;

import org.jetbrains.annotations.NotNull;

public interface MongoConfig {

    @NotNull String uri();

    @NotNull String database();

    boolean useTransactions();

}

package net.hollowcube.chat.storage;

import net.hollowcube.common.config.MongoConfig;
import org.jetbrains.annotations.NotNull;

public class MockMongoConfig implements MongoConfig {
    @Override
    public @NotNull String uri() {
        return "";
    }

    @Override
    public @NotNull String database() {
        return "mapmaker";
    }

    @Override
    public boolean useTransactions() {
        return false;
    }
}


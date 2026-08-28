package net.hollowcube.apiserver.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/// The connection pools these processes open, all configured the same way.
public final class Pools {

    public static HikariDataSource postgres(PostgresUri uri, String name) {
        var config = new HikariConfig();
        config.setJdbcUrl(uri.jdbcUrl());
        config.setUsername(uri.user());
        config.setPassword(uri.password());
        config.setPoolName(name);
        // Hikari waits 30s for a connection by default, which is longer than /ready is worth
        // keeping a probe waiting and longer than an internal call should hang before it fails.
        config.setConnectionTimeout(5_000);
        // A database that is briefly unreachable at startup is something to report through /ready,
        // not to crash over, so the pool fills lazily rather than failing construction.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private Pools() {
    }
}

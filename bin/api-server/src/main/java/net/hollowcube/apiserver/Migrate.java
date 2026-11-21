package net.hollowcube.apiserver;

import net.hollowcube.apiserver.common.PostgresUri;
import net.hollowcube.sqlgen.runtime.Migrator;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Properties;

/// `./gradlew :bin:api-server:migrate`: applies `modules/api`'s migrations to `DATABASE_URL`, or the
/// compose postgres when it is unset. `--baseline N` records migrations up to `N` as applied without
/// running them, for a database whose migrations were applied by hand.
public final class Migrate {

    public static void main(String[] args) throws Exception {
        if (args.length != 1 && !(args.length == 3 && args[1].equals("--baseline")))
            throw new IllegalArgumentException("usage: Migrate <migrations dir> [--baseline N]");
        var dir = Path.of(args[0]);

        var url = System.getenv("DATABASE_URL");
        var uri = PostgresUri.parse(url == null || url.isBlank() ? PostgresUri.LOCAL_DEV : url);
        var props = new Properties();
        if (uri.user() != null) props.setProperty("user", uri.user());
        if (uri.password() != null) props.setProperty("password", uri.password());

        try (var conn = DriverManager.getConnection(uri.jdbcUrl(), props)) {
            if (args.length == 3) {
                for (var migration : Migrator.baseline(conn, dir, Integer.parseInt(args[2])))
                    System.out.println("baselined " + migration.file().getFileName());
            } else if (Migrator.migrate(conn, dir).isEmpty()) {
                System.out.println("up to date");
            }
        }
    }
}

package net.hollowcube.apiserver;

import org.jetbrains.annotations.Nullable;

import java.net.URI;

/// A libpq connection URI — `postgres://user:pass@host:5432/db?sslmode=disable`, which is the shape
/// the credentials are stored in — split into the three things Hikari wants, because pgjdbc reads
/// neither the user nor the password out of a URL.
///
/// A `jdbc:` URL is taken as already being one and passed through untouched.
public record PostgresUri(String jdbcUrl, @Nullable String user, @Nullable String password) {
    private static final String JDBC_PREFIX = "jdbc:postgresql://";

    public static PostgresUri parse(String uri) {
        if (uri.startsWith("jdbc:")) return new PostgresUri(uri, null, null);

        var parsed = URI.create(uri);
        var scheme = parsed.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme))
            throw new IllegalArgumentException("not a postgres uri: " + scheme + "://...");
        if (parsed.getHost() == null)
            throw new IllegalArgumentException("postgres uri has no host");

        var jdbcUrl = new StringBuilder(JDBC_PREFIX).append(parsed.getHost());
        if (parsed.getPort() != -1) jdbcUrl.append(':').append(parsed.getPort());
        jdbcUrl.append(parsed.getRawPath() == null ? "/" : parsed.getRawPath());
        if (parsed.getRawQuery() != null) jdbcUrl.append('?').append(parsed.getRawQuery());

        var userInfo = parsed.getUserInfo();
        if (userInfo == null) return new PostgresUri(jdbcUrl.toString(), null, null);
        var split = userInfo.indexOf(':');
        return split < 0
            ? new PostgresUri(jdbcUrl.toString(), userInfo, null)
            : new PostgresUri(jdbcUrl.toString(), userInfo.substring(0, split), userInfo.substring(split + 1));
    }
}

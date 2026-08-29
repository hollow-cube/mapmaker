package net.hollowcube.apiserver.common;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/// The secrets the vault agent sidecar renders beside the process.
///
/// Same path, same `key = value` shape and the same vault keys the Go api-server reads, so the two
/// can run off one vault role instead of needing the database credentials copied to a second secret
/// that then has to be kept in step.
///
/// Every lookup reads the environment first, so a run with no sidecar — local, or a test — needs
/// nothing but the environment variable, and a deployed one can still be overridden from the chart.
public final class VaultSecrets {
    private static final Logger logger = LoggerFactory.getLogger(VaultSecrets.class);
    private static final Path PATH = Path.of("/vault/secrets/service");

    private final Map<String, String> values;
    private final boolean present;

    private VaultSecrets(Map<String, String> values, boolean present) {
        this.values = values;
        this.present = present;
    }

    public static VaultSecrets load() {
        return load(PATH);
    }

    public static VaultSecrets load(Path path) {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            // Not an error: nothing outside the cluster has a sidecar to read.
            logger.info("no vault secrets at {}, reading the environment only", path);
            return new VaultSecrets(Map.of(), false);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read vault secrets at " + path, e);
        }

        var values = new HashMap<String, String>();
        for (var line : content.lines().toList()) {
            var trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            var split = trimmed.indexOf('=');
            if (split < 0) continue;
            values.put(trimmed.substring(0, split).trim(), trimmed.substring(split + 1).trim());
        }
        logger.info("read {} vault secrets from {}", values.size(), path);
        return new VaultSecrets(values, true);
    }

    /// Whether a sidecar rendered anything at all, which is to say whether this is a process in
    /// the cluster rather than on someone's machine.
    public boolean present() {
        return present;
    }

    public @Nullable String get(String key, String env) {
        var fromEnv = System.getenv(env);
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv;
        var value = values.get(key);
        return value == null || value.isBlank() ? null : value;
    }

    public String get(String key, String env, String fallback) {
        var value = get(key, env);
        return value == null ? fallback : value;
    }

    public String require(String key, String env) {
        var value = get(key, env);
        if (value == null) throw new IllegalStateException("neither $" + env + " nor the vault key '" + key + "' is set");
        return value;
    }
}

package net.hollowcube.apiserver.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultSecretsTest {

    @Test
    void testAgentTemplateShape(@TempDir Path dir) throws IOException {
        // What the agent sidecar's template renders: one `key = value` per secret, blank last line.
        var file = Files.writeString(dir.resolve("service"), """
            postgres.maps_uri = postgres://mapmaker:hunter2@postgres.mapmaker:5432/map-service
            posthog.personal_api_key = phx_abc

            """);

        var secrets = VaultSecrets.load(file);
        assertEquals("postgres://mapmaker:hunter2@postgres.mapmaker:5432/map-service",
            secrets.get("postgres.maps_uri", "DATABASE_URL_NOT_SET"));
        assertEquals("phx_abc", secrets.get("posthog.personal_api_key", "NOT_SET"));
    }

    @Test
    void testValueMayContainEquals(@TempDir Path dir) throws IOException {
        var file = Files.writeString(dir.resolve("service"), "keyring.key = aGVsbG8=\n");

        assertEquals("aGVsbG8=", VaultSecrets.load(file).get("keyring.key", "NOT_SET"));
    }

    @Test
    void testMissingFileIsNotAnError(@TempDir Path dir) {
        var secrets = VaultSecrets.load(dir.resolve("no-sidecar-here"));

        assertNull(secrets.get("postgres.maps_uri", "NOT_SET"));
        assertEquals("9124", secrets.get("http.port", "NOT_SET", "9124"));
        assertThrows(IllegalStateException.class, () -> secrets.require("postgres.maps_uri", "NOT_SET"));
    }
}

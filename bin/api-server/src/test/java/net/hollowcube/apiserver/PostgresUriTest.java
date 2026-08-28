package net.hollowcube.apiserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresUriTest {

    @Test
    void testFullUri() {
        var uri = PostgresUri.parse("postgres://mapmaker:hunter2@postgres.mapmaker:5432/map-service?sslmode=disable");
        assertEquals("jdbc:postgresql://postgres.mapmaker:5432/map-service?sslmode=disable", uri.jdbcUrl());
        assertEquals("mapmaker", uri.user());
        assertEquals("hunter2", uri.password());
    }

    @Test
    void testPostgresqlScheme() {
        var uri = PostgresUri.parse("postgresql://postgres.mapmaker/map-service");
        assertEquals("jdbc:postgresql://postgres.mapmaker/map-service", uri.jdbcUrl());
        assertNull(uri.user());
        assertNull(uri.password());
    }

    @Test
    void testUserWithoutPassword() {
        var uri = PostgresUri.parse("postgres://mapmaker@postgres.mapmaker:5432/map-service");
        assertEquals("mapmaker", uri.user());
        assertNull(uri.password());
    }

    @Test
    void testJdbcUrlPassesThrough() {
        var uri = PostgresUri.parse("jdbc:postgresql://localhost:5432/map-service");
        assertEquals("jdbc:postgresql://localhost:5432/map-service", uri.jdbcUrl());
        assertNull(uri.user());
    }

    @Test
    void testRejectsOtherSchemes() {
        assertThrows(IllegalArgumentException.class, () -> PostgresUri.parse("mysql://localhost/map-service"));
    }
}

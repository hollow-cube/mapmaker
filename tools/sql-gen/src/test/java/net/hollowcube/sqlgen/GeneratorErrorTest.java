package net.hollowcube.sqlgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The cases the generator is supposed to refuse.
///
/// Each one is something that would otherwise become a silent wrong answer at runtime — a column
/// typed `Object`, a nullable value in a primitive, a query that never matched the schema — so the
/// error message naming the query is the feature.
class GeneratorErrorTest {

    private static final String MIGRATION = """
        create table thing
        (
            id    int primary key,
            name  varchar not null,
            note  varchar
        );
        """;

    @TempDir
    Path dir;

    private String generate(String queries) throws IOException {
        var migrations = Files.createDirectories(dir.resolve("migrations"));
        var queryDir = Files.createDirectories(dir.resolve("queries"));
        Files.writeString(migrations.resolve("0001_thing.sql"), MIGRATION);
        Files.writeString(queryDir.resolve("things.sql"), queries);

        var failure = assertThrows(GenException.class,
            () -> SqlGen.generate(migrations, queryDir, "test.db", "TestDatabase"));
        return failure.getMessage();
    }

    @Test
    void positionalPlaceholdersAreRejected() throws IOException {
        var message = generate("""
            -- name: getThing :one
            select thing.* from thing where id = $1;
            """);

        assertTrue(message.contains("positional placeholder"), message);
    }

    @Test
    void aBareStarCannotNameItsEmbed() throws IOException {
        var message = generate("""
            -- name: listThings :many
            select * from thing;
            """);

        assertTrue(message.contains("1 embedded table but the select list has 0 'ref.*' markers"), message);
    }

    @Test
    void anUnmappableTypeNamesTheColumn() throws IOException {
        var message = generate("""
            -- name: getInterval :one
            select interval '1 day' as gap;
            """);

        assertTrue(message.contains("no Java type for Postgres type 'interval'"), message);
        assertTrue(message.contains("getInterval"), message);
    }

    @Test
    void execMustNotReturnColumns() throws IOException {
        var message = generate("""
            -- name: insertThing :exec
            insert into thing (id, name) values ($id, $name) returning thing.*;
            """);

        assertTrue(message.contains("tagged :exec but returns columns"), message);
    }

    @Test
    void aStatementWithNoResultCannotBeTaggedOne() throws IOException {
        var message = generate("""
            -- name: insertThing :one
            insert into thing (id, name) values ($id, $name);
            """);

        assertTrue(message.contains("returns no columns; tag it :exec"), message);
    }

    @Test
    void aDirectiveThatNamesNothingIsAnError() throws IOException {
        var message = generate("""
            -- name: getThing :one
            -- nullable: missing
            select thing.* from thing where id = $id;
            """);

        assertTrue(message.contains("does not name a result column"), message);
    }

    @Test
    void notNullOnAColumnTheServerAlreadyKnowsIsRedundant() throws IOException {
        var message = generate("""
            -- name: getThingName :one
            -- not-null: name
            select name from thing where id = $id;
            """);

        assertTrue(message.contains("redundant"), message);
    }

    @Test
    void nullableOnAnAlreadyNullableColumnIsRedundant() throws IOException {
        var message = generate("""
            -- name: getThingNote :one
            -- nullable: note
            select note from thing where id = $id;
            """);

        assertTrue(message.contains("redundant"), message);
    }

    @Test
    void aQueryThatDoesNotMatchTheSchemaFailsGeneration() throws IOException {
        var message = generate("""
            -- name: getThing :one
            select thing.missing from thing;
            """);

        assertTrue(message.contains("does not describe against the schema"), message);
        assertTrue(message.contains("getThing"), message);
    }

    @Test
    void aFileWithNoHeaderIsAnError() throws IOException {
        var message = generate("select 1;\n");

        assertTrue(message.contains("SQL before the first '-- name:' header"), message);
    }

    @Test
    void aQueryCannotHaveTwoHolesOfTheSameKind() throws IOException {
        var message = generate("""
            -- name: listThings :many
            select thing.* from thing /* where */ /* where */;
            """);

        assertTrue(message.contains("more than one /* where */ hole"), message);
    }
}

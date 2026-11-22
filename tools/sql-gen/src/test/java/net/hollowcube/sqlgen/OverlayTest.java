package net.hollowcube.sqlgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayTest {

    @TempDir
    Path dir;

    private Map<String, String> generate(String overlay) throws IOException {
        var migrations = Files.createDirectories(dir.resolve("migrations"));
        var queries = Files.createDirectories(dir.resolve("queries"));
        Files.writeString(migrations.resolve("0001_thing.sql"), """
            create table thing
            (
                id    int primary key,
                name  text not null,
                kind  text not null default 'plain',
                note  text
            );
            create table other (id int primary key);
            """);
        Files.writeString(queries.resolve("things.sql"), """
            -- name: getThing :one
            select thing.* from thing where id = $id;
            """);
        var file = dir.resolve("overlay.sql");
        Files.writeString(file, overlay);
        return SqlGen.generate(migrations, file, queries, "test.db", "TestDatabase");
    }

    private String refused(String overlay) {
        return assertThrows(GenException.class, () -> generate(overlay)).getMessage();
    }

    @Test
    void droppedColumns_leaveTheTableRecord() throws IOException {
        var record = generate("""
            -- comments are fine
            alter table thing drop column note;
            alter table thing drop column if exists kind;
            drop table other;
            """).get("test/db/Thing.java");

        assertTrue(record.contains("name"), record);
        assertFalse(record.contains("note"), record);
        assertFalse(record.contains("kind"), record);
    }

    @Test
    void anythingButADropOfANullableOrDefaultedColumnIsRefused() {
        var message = refused("alter table thing drop column name;");
        assertTrue(message.contains("drops thing.name, which is not null without a default"), message);

        message = refused("alter table thing drop column gone;");
        assertTrue(message.contains("drops thing.gone, which does not exist"), message);

        message = refused("alter table thing add column extra text;");
        assertTrue(message.contains("may only drop"), message);
    }
}

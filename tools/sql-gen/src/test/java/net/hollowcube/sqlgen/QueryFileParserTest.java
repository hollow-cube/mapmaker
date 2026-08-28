package net.hollowcube.sqlgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// The placeholder rewrite, which is the one piece of the pipeline that reads SQL text rather than
/// asking the server about it.
class QueryFileParserTest {

    @TempDir
    Path dir;

    private QueryFile parse(String text) {
        try {
            var file = dir.resolve("things.sql");
            Files.writeString(file, text);
            return QueryFileParser.parse(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void aOneThatIsOnlyAggregatesAlwaysHasItsRow() {
        assertTrue(parse("-- name: q :one\nselect count(*) from t;").queries().getFirst().alwaysOneRow());
        assertTrue(parse("-- name: q :one\nselect count(*) as total, max(id) from t where x = $x;").queries().getFirst().alwaysOneRow());
        assertTrue(parse("-- name: q :one\n-- a comment\nselect sum(n)\nfrom t;").queries().getFirst().alwaysOneRow());

        // One row per group, one row per matched id, a CTE, a union: not obviously one row.
        assertFalse(parse("-- name: q :one\nselect count(*) from t group by x;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect * from t where id = (select max(id) from t);").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nwith c as (select 1) select count(*) from c;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*) from a union select count(*) from b;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*), id from t;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :many\nselect count(*) from t;").queries().getFirst().alwaysOneRow());

        // Zero rows are as possible as one here: limits, windows, and a group by however spelled.
        assertFalse(parse("-- name: q :one\nselect count(*) from t limit 0;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*) from t offset 1;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*) from t fetch first 0 rows only;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*) over () from t;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect max(x) over (partition by y) from t;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*)\nfrom t\ngroup\n  by x;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*)from t GROUP  BY x;").queries().getFirst().alwaysOneRow());
        assertFalse(parse("-- name: q :one\nselect count(*) from t where s = 'a--b' group by x;").queries().getFirst().alwaysOneRow());
        // And a literal that merely mentions one is not one.
        assertTrue(parse("-- name: q :one\nselect count(*) from t where s = 'group by';").queries().getFirst().alwaysOneRow());
    }

    @Test
    void theFileNameIsTheGroup() {
        assertEquals("things", parse("""
            -- name: q :many
            select 1;
            """).group());
    }

    @Test
    void placeholdersBecomeQuestionMarksInOrderOfFirstAppearance() {
        var query = parse("""
            -- name: q :many
            select * from thing where name = $name and id > $id and note = $name;
            """).queries().getFirst();

        assertEquals(List.of("name", "id"), query.params());
        assertEquals(List.of(0, 1, 0), query.binds());
        assertEquals("select * from thing where name = ? and id > ? and note = ?", query.sql());
    }

    @Test
    void stringsAndCommentsKeepTheirDollarSigns() {
        var query = parse("""
            -- name: q :many
            select '$notAParam' as literal, /* $alsoNot */ $real as bound
            from thing -- $neither
            """).queries().getFirst();

        assertEquals(List.of("real"), query.params());
        assertTrue(query.sql().contains("'$notAParam'"), query.sql());
        assertTrue(query.sql().contains("/* $alsoNot */"), query.sql());
        assertTrue(query.sql().contains("-- $neither"), query.sql());
    }

    @Test
    void dollarQuotedBodiesAreNotPlaceholders() {
        var query = parse("""
            -- name: q :many
            select $tag$ raw $body$ text $tag$ as quoted, $real as bound from thing;
            """).queries().getFirst();

        assertEquals(List.of("real"), query.params());
    }

    @Test
    void directivesAreStrippedFromTheStatement() {
        var query = parse("""
            -- name: q :many
            -- nullable: a, b
            -- not-null: c
            -- an ordinary comment
            select 1;
            """).queries().getFirst();

        assertEquals(java.util.Set.of("a", "b"), query.nullable());
        assertEquals(java.util.Set.of("c"), query.notNull());
        assertEquals("-- an ordinary comment\nselect 1", query.sql());
    }

    @Test
    void oneFileHoldsManyQueries() {
        var file = parse("""
            -- name: first :one
            select 1;

            -- name: second :exec
            delete from thing;
            """);

        assertEquals(List.of("first", "second"), file.queries().stream().map(QueryFile.Query::name).toList());
        assertEquals(QueryFile.Tag.ONE, file.queries().getFirst().tag());
        assertEquals(QueryFile.Tag.EXEC, file.queries().getLast().tag());
    }

    @Test
    void holeMarkersAreStrippedAndRecordedWithTheirBindPosition() {
        var query = parse("""
            -- name: q :many
            select * from thing
            /*  Where  */
            /* order by */
            limit $limit
            """).queries().getFirst();

        assertEquals(List.of(QueryFile.Hole.Kind.WHERE, QueryFile.Hole.Kind.ORDER_BY),
            query.holes().stream().map(QueryFile.Hole::kind).toList());
        assertEquals(List.of(0, 0), query.holes().stream().map(QueryFile.Hole::bindOffset).toList());
        assertTrue(!query.sql().contains("where"), query.sql());
        assertTrue(query.sql().endsWith("limit ?"), query.sql());
    }

    @Test
    void aHoleAfterAPlaceholderRemembersHowManyCameBefore() {
        var query = parse("""
            -- name: q :many
            select * from thing where id > $id /* order by */ limit $limit;
            """).queries().getFirst();

        assertEquals(1, query.holes().getFirst().bindOffset());
    }
}

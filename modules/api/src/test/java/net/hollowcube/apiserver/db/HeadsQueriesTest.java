package net.hollowcube.apiserver.db;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the generated head_db queries against a real Postgres built from the same migrations the
/// generator described them against.
///
/// This is the check that the generated code is not merely well-formed: the row shapes, the column
/// offsets behind `head_db.*`, and the array mapping all have to line up with what the server
/// actually sends back. The search cases also pin what `head_db_search` makes findable, since the
/// tsquery a caller passes is meaningless without knowing what went into the document.
class HeadsQueriesTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    @BeforeEach
    void seed() {
        TEST_DB.seed("""
            insert into head_db (id, category, name, tags, texture) values
                (1, 'mob', 'Creeper Head', array['green', 'scary'], 'tex-creeper'),
                (2, 'mob', 'Zombie Head', array['green'], 'tex-zombie'),
                (3, 'block', 'Stone Block', array[]::varchar[], 'tex-stone')""");
    }

    @Test
    void getRandomHeads_mapsEveryColumnOfTheEmbeddedTable() {
        HeadDb creeper = db.heads.getRandomHeads(3).stream()
            .map(HeadsQueries.GetRandomHeadsRow::headDb)
            .filter(head -> head.id() == 1).findFirst().orElseThrow();

        assertEquals("mob", creeper.category());
        assertEquals("Creeper Head", creeper.name());
        assertEquals(List.of("green", "scary"), creeper.tags());
        assertEquals("tex-creeper", creeper.texture());
    }

    @Test
    void getRandomHeads_countsTheWholeTableNotThePage() {
        List<HeadsQueries.GetRandomHeadsRow> rows = db.heads.getRandomHeads(2);

        assertEquals(2, rows.size());
        for (HeadsQueries.GetRandomHeadsRow row : rows) assertEquals(3, row.totalCount());
    }

    @Test
    void getHeadsWithSearch_matchesOnName() {
        List<HeadsQueries.GetHeadsWithSearchRow> rows = db.heads.getHeadsWithSearch("creeper", 10, 0);

        assertEquals(1, rows.size());
        assertEquals("Creeper Head", rows.getFirst().headDb().name());
        assertEquals(1, rows.getFirst().totalCount());
    }

    @Test
    void getHeadsWithSearch_matchesOnTags() {
        List<HeadsQueries.GetHeadsWithSearchRow> rows = db.heads.getHeadsWithSearch("scary", 10, 0);

        assertEquals(1, rows.size());
        assertEquals("Creeper Head", rows.getFirst().headDb().name());
    }

    @Test
    void getHeadsWithSearch_matchesAPrefixToken() {
        List<HeadsQueries.GetHeadsWithSearchRow> rows = db.heads.getHeadsWithSearch("creep:*", 10, 0);

        assertEquals(1, rows.size());
        assertEquals("Creeper Head", rows.getFirst().headDb().name());
    }

    @Test
    void getHeadsWithSearch_andsTheTokensTogether() {
        assertEquals(2, db.heads.getHeadsWithSearch("head:*", 10, 0).size());
        assertEquals(1, db.heads.getHeadsWithSearch("zombie & head:*", 10, 0).size());
        assertEquals(0, db.heads.getHeadsWithSearch("zombie & block:*", 10, 0).size());
    }

    @Test
    void getHeadsWithSearch_carriesTheWindowCountPastTheLimit() {
        List<HeadsQueries.GetHeadsWithSearchRow> rows = db.heads.getHeadsWithSearch("head:*", 1, 0);

        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst().totalCount());
    }

    @Test
    void getHeadsWithSearch_offsetWalksTheMatches() {
        HeadsQueries.GetHeadsWithSearchRow first = db.heads.getHeadsWithSearch("head:*", 1, 0).getFirst();
        HeadsQueries.GetHeadsWithSearchRow second = db.heads.getHeadsWithSearch("head:*", 1, 1).getFirst();

        assertEquals(2, first.totalCount());
        assertEquals(2, second.totalCount());
        assertTrue(first.headDb().id() != second.headDb().id());
    }

    @Test
    void getHeadsWithSearch_answersNothingRatherThanEverythingWhenNothingMatches() {
        assertEquals(List.of(), db.heads.getHeadsWithSearch("piglin:*", 10, 0));
    }

    @Test
    void getHeadsWithCategory_filtersAndCountsThatCategoryOnly() {
        List<HeadsQueries.GetHeadsWithCategoryRow> rows = db.heads.getHeadsWithCategory("mob", 10, 0);

        assertEquals(2, rows.size());
        assertEquals(2, rows.getFirst().totalCount());
        for (HeadsQueries.GetHeadsWithCategoryRow row : rows) assertEquals("mob", row.headDb().category());
    }

    @Test
    void getHeadsWithCategory_ordersByNameSoPagesDoNotOverlap() {
        List<HeadsQueries.GetHeadsWithCategoryRow> page = db.heads.getHeadsWithCategory("mob", 10, 0);
        assertEquals(List.of("Creeper Head", "Zombie Head"), page.stream().map(row -> row.headDb().name()).toList());

        assertEquals("Zombie Head", db.heads.getHeadsWithCategory("mob", 1, 1).getFirst().headDb().name());
    }

    @Test
    void getHeadsWithCategory_readsAnEmptyArrayAsAnEmptyList() {
        HeadsQueries.GetHeadsWithCategoryRow stone = db.heads.getHeadsWithCategory("block", 10, 0).getFirst();

        assertEquals(List.of(), stone.headDb().tags());
        assertEquals("Stone Block", stone.headDb().name());
    }
}

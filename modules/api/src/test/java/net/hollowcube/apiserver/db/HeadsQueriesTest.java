package net.hollowcube.apiserver.db;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Drives the generated head_db queries against a real Postgres built from the same migrations the
/// generator described them against. The search case pins what `head_db_search` makes findable,
/// since the tsquery a caller passes is meaningless without knowing what went into the document.
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

    private List<String> search(String query) {
        return db.heads.getHeadsWithSearch(query, 0, 10).stream().map(row -> row.headDb().name()).toList();
    }

    @Test
    void getRandomHeads_countsTheWholeTableNotThePage() {
        List<HeadsQueries.GetRandomHeadsRow> rows = db.heads.getRandomHeads(2);

        assertEquals(2, rows.size());
        for (HeadsQueries.GetRandomHeadsRow row : rows) assertEquals(3, row.totalCount());
    }

    @Test
    void getHeadsWithSearch_matchesNameAndTags() {
        assertEquals(List.of("Creeper Head"), search("creeper"));
        assertEquals(List.of("Creeper Head"), search("scary"));
        assertEquals(List.of("Creeper Head"), search("creep:*"));
        assertEquals(2, search("head:*").size());
        assertEquals(List.of("Zombie Head"), search("zombie & head:*"));
        assertEquals(List.of(), search("zombie & block:*"));
        assertEquals(List.of(), search("piglin:*"));
    }

    @Test
    void getHeadsWithSearch_pagesWithTheWindowCount() {
        HeadsQueries.GetHeadsWithSearchRow first = db.heads.getHeadsWithSearch("head:*", 0, 1).getFirst();
        HeadsQueries.GetHeadsWithSearchRow second = db.heads.getHeadsWithSearch("head:*", 1, 1).getFirst();

        assertEquals(2, first.totalCount());
        assertEquals(2, second.totalCount());
        assertNotEquals(first.headDb().id(), second.headDb().id());
    }

    @Test
    void getHeadsWithCategory_filtersCountsAndPagesByName() {
        List<HeadsQueries.GetHeadsWithCategoryRow> page = db.heads.getHeadsWithCategory("mob", 0, 10);
        assertEquals(List.of("Creeper Head", "Zombie Head"), page.stream().map(row -> row.headDb().name()).toList());
        for (HeadsQueries.GetHeadsWithCategoryRow row : page) assertEquals(2, row.totalCount());

        assertEquals("Zombie Head", db.heads.getHeadsWithCategory("mob", 1, 1).getFirst().headDb().name());
    }
}

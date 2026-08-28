package net.hollowcube.sqlgen.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TsQueryTest {

    @Test
    void everyWordHasToMatchAndTheLastOneIsLeftOpen() {
        assertEquals("creeper:*", TsQuery.of("creeper"));
        assertEquals("green & creep:*", TsQuery.of("green creep"));
    }

    @Test
    void caseAndSeparatorsAreNormalisedAway() {
        assertEquals("iron & golem:*", TsQuery.of("  Iron   GOLEM  "));
        assertEquals("red & stone:*", TsQuery.of("red-stone"));
    }

    /// The whole reason this exists rather than passing the text to `to_tsquery`: tsquery has its
    /// own operators, and someone typing one is searching for a word, not writing a query.
    @Test
    void tsqueryOperatorsInTheTextAreJustSeparators() {
        assertEquals("a & b:*", TsQuery.of("a & b"));
        assertEquals("a & b:*", TsQuery.of("a | !b"));
        assertEquals("drop:*", TsQuery.of("'drop'"));
    }

    @Test
    void textWithNoWordInItIsNotASearch() {
        assertNull(TsQuery.of(""));
        assertNull(TsQuery.of("   "));
        assertNull(TsQuery.of("!!!"));
    }

    @Test
    void digitsStayWithTheWordTheyWereTypedAgainst() {
        assertEquals("head2:*", TsQuery.of("head2"));
    }
}

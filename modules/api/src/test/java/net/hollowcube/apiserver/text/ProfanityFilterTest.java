package net.hollowcube.apiserver.text;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfanityFilterTest {

    private static String matched(String text) {
        var result = ProfanityFilter.test(text);
        return result.matched() ? result.matches().getFirst().term() : "";
    }

    private static void passes(String... texts) {
        for (var text : texts)
            assertEquals("", matched(text), "'" + text + "' should pass");
    }

    private static void flags(String term, String... texts) {
        for (var text : texts)
            assertEquals(term, matched(text), "'" + text + "' should flag " + term);
    }

    @Test
    void plainWords() {
        flags("fuck", "fuck", "FUCK", "Fuck this", "this is fucked", "motherfucker");
        flags("shit", "shithead");
        passes("hello world", "", "   ", "!!!", "the quick brown fox");
    }

    @Test
    void wordsThatMerelyContainATerm_pass() {
        passes("grape juice", "the cockpit", "a cocktail", "raccoon", "scunthorpe", "read the document", "cumulus",
            "the therapist", "therapeutic", "pakistan", "japan", "thorny", "peacock", "flanged", "muffin", "swank",
            "ashkenazi", "bastardized", "retardant", "the basement", "clitheroe", "dickens", "vandyke",
            "you scum", "scummy", "the four horsemen", "puberty", "pachinko", "poppycock", "cockles", "riddick");
        flags("nigger", "sniggers", "sniggering"); // deliberately still blocked
    }

    @Test
    void leetspeakAndSymbols() {
        flags("shit", "sh1t", "$hit", "5h!t", "sh|t", "$h17");
        flags("fuck", "f u c k", "f.u.c.k", "f-u-c-k", "f_u_c_k", "fu(k", "f*u*c*k");
        flags("cock", "c()ck", "c[]ck", "c{}ck", "c<>ck", "c0ck");
        flags("asshol", "@sshole", "a$$hole");
        flags("cunt", "(unt", "<unt", "{unt");
        flags("penis", "p3n1s", "pen!s");
    }

    @Test
    void accentsAndScripts() {
        flags("fuck", "fück", "fùck", "FÜCK", "fu\u0308ck"); // precomposed and combining
        flags("fuck", "ｆｕｃｋ"); // fullwidth
        flags("shit", "ѕhit", "shіt", "ѕһіт"); // cyrillic lookalikes
        flags("rape", "rаpe", "rαpe"); // cyrillic а, greek α
        flags("fuck", "fuck😀", "😀fuck", "fu😀ck");
        passes("привет мир", "こんにちは", "日本語");
    }

    @Test
    void termAcrossTwoInnocentWords_passes() {
        // The reason this filter exists: each of these spells a term across a word seam.
        passes("elytra pearl", "elytra people", "elytra pe", "mass hit", "grass hit me", "this hitbox", "finish it",
            "publish it", "push it", "english it", "who responded", "who returned", "who revolted", "open is",
            "for a permanent", "class ic", "for a pen", "the class hit", "brush it off", "was hit", "elytra pearl fun");
    }

    @Test
    void termSpelledOutInPieces_flags() {
        flags("rape", "r a p e", "ra pe", "r ape", "rap e", "r-a-p-e");
        flags("shit", "sh it", "shi t", "s h i t", "s.h.i.t");
        flags("nigger", "nig ger", "n i g g e r", "ni gger");
        flags("fuck", "fu ck", "fuc k you", "f uck");
        flags("asshol", "ass hole"); // stem, ending one char short of the next word
        flags("shit", "bull shit", "horse shit"); // a negative is one word
        flags("spunk", "spun k");
        flags("kissmy", "kiss my", "kiss my ass");
    }

    @Test
    void pieceEndingDeepInTheNextWord_isNotAMatch() {
        // Start-aligned, so the only thing that saves these is how much of the last word is left.
        passes("who rest", "who resets", "as shoals", "ni ggle", "ass holes");
        flags("whore", "who re", "who rev"); // one char over is still the spelled-out shape
    }

    @Test
    void longestTermWins() {
        flags("nigger", "nigger", "niggers");
        flags("nigga", "nigga");
        flags("nigg", "niggs");
        flags("clitoris", "clitoris"); // not the shorter clit
        flags("kluklux", "kluklux");
        flags("klukluxklan", "klukluxklan");
    }

    @Test
    void negatedShortTermDoesNotHideOrFakeALongerOne() {
        // clit is negated by clitheroe and is a prefix of clitoris: a negated short term must
        // neither be reported itself nor hide the longer term through the same node.
        passes("clitheroe", "in clitheroe town");
        flags("clitoris", "clitoris");
    }

    @Test
    void everyTermFlagsItself_andEveryNegativePasses() {
        for (var entry : Profanities.TERM_TRIE.terms().entrySet()) {
            var term = entry.getKey();
            var matches = ProfanityFilter.test(term).matches();
            assertFalse(matches.isEmpty(), term + " should flag");
            assertEquals(term, matches.getFirst().term(), term + " should flag as itself");
            for (var negative : entry.getValue()) {
                for (var match : ProfanityFilter.test(negative).matches())
                    assertNotEquals(term, match.term(), negative + " should not flag " + term);
            }
        }
    }

    @Test
    void manyMatches_allReported() {
        var result = ProfanityFilter.test("fuck this shit, you cunt");
        assertEquals(List.of("fuck", "shit", "cunt"), result.matches().stream().map(ProfanityFilter.Match::term).toList());
        assertEquals("**** this ****, you ****", result.censored('*'));
    }

    @Test
    void matchRangesAndMask_areInOriginalChars() {
        var result = ProfanityFilter.test("well fuck");
        assertEquals(List.of(new ProfanityFilter.Match("fuck", 5, 9)), result.matches());
        assertEquals("well ****", result.censored('*'));

        assertEquals("****", ProfanityFilter.test("fück").censored('*'));
        assertEquals("*****", ProfanityFilter.test("fu\u0308ck").censored('*')); // combining mark inside the span
        assertEquals("*******", ProfanityFilter.test("r a p e").censored('*')); // spelled out: the whole span
        assertEquals("*****!", ProfanityFilter.test("c()ck!").censored('*'));
        assertEquals("😀 ****", ProfanityFilter.test("😀 fuck").censored('*')); // surrogate pair before it
        assertEquals("****😀****", ProfanityFilter.test("fuck😀shit").censored('*'));
        assertEquals("**** ****", ProfanityFilter.test("ｆｕｃｋ shit").censored('*'));
    }

    @Test
    void nothingMatched_isEmptyMask() {
        var result = ProfanityFilter.test("hello there");
        assertFalse(result.matched());
        assertEquals(new BitSet(), result.mask());
        assertEquals("hello there", result.censored('*'));
    }

    @Test
    void longCleanText() {
        var text = """
            Rebellious subjects, enemies to peace,
            Profaners of this neighbour-stained steel,—
            Will they not hear? What, ho! You men, you beasts,
            That quench the fire of your pernicious rage
            With purple fountains issuing from your veins,
            On pain of torture, from those bloody hands
            Throw your mistemper'd weapons to the ground
            And hear the sentence of your moved prince.
            Three civil brawls, bred of an airy word,
            By thee, old Capulet, and Montague,
            Have thrice disturb'd the quiet of our streets,
            And made Verona's ancient citizens
            Cast by their grave beseeming ornaments,
            To wield old partisans, in hands as old,
            Canker'd with peace, to part your canker'd hate.
            If ever you disturb our streets again,
            Your lives shall pay the forfeit of the peace.
            Who set this ancient quarrel new abroach?
            Speak, nephew, were you by when it began?
            Here were the servants of your adversary
            And yours, close fighting ere I did approach.
            I drew to part them, in the instant came
            The fiery Tybalt, with his sword prepar'd,
            Which, as he breath'd defiance to my ears,
            He swung about his head, and cut the winds,
            Who nothing hurt withal, hiss'd him in scorn.
            Many a morning hath he there been seen,
            With tears augmenting the fresh morning's dew,
            Adding to clouds more clouds with his deep sighs;
            Shuts up his windows, locks fair daylight out
            And makes himself an artificial night.
            Could we but learn from whence his sorrows grow,
            We would as willingly give cure as know.
            """;
        var result = ProfanityFilter.test(text);
        assertFalse(result.matched(), () -> result.matches().toString());
    }
}

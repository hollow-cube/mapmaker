package net.hollowcube.scripting.docs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavadocTagParserTest {

    @Test
    void nullCommentYieldsEmpty() {
        var docs = JavadocTagParser.parse(null);
        assertEquals("", docs.description());
        assertTrue(docs.params().isEmpty());
        assertTrue(docs.returns().isEmpty());
        assertTrue(docs.generics().isEmpty());
        assertTrue(docs.diagnostics().isEmpty());
    }

    @Test
    void blankCommentYieldsEmpty() {
        var docs = JavadocTagParser.parse("");
        assertEquals("", docs.description());
        assertTrue(docs.diagnostics().isEmpty());
    }

    @Test
    void descriptionOnly() {
        var docs = JavadocTagParser.parse("Find a player by name.");
        assertEquals("Find a player by name.", docs.description());
        assertTrue(docs.params().isEmpty());
    }

    @Test
    void multilineDescriptionPreservesParagraphs() {
        var raw = "First line.\n\nSecond paragraph.";
        var docs = JavadocTagParser.parse(raw);
        assertEquals("First line.\n\nSecond paragraph.", docs.description());
    }

    @Test
    void paramTagSimple() {
        var docs = JavadocTagParser.parse("@luaParam name string");
        assertEquals(1, docs.params().size());
        var p = docs.params().get(0);
        assertEquals("name", p.name());
        assertFalse(p.optional());
        assertEquals("string", p.typeExpr());
    }

    @Test
    void paramTagOptional() {
        var docs = JavadocTagParser.parse("@luaParam name? string");
        var p = docs.params().get(0);
        assertEquals("name", p.name());
        assertTrue(p.optional());
        assertEquals("string", p.typeExpr());
    }

    @Test
    void paramTagComplexTypeExpr() {
        var docs = JavadocTagParser.parse("@luaParam target EventSource<(Player, World)>");
        var p = docs.params().get(0);
        assertEquals("target", p.name());
        assertEquals("EventSource<(Player, World)>", p.typeExpr());
    }

    @Test
    void returnTagSingle() {
        var docs = JavadocTagParser.parse("@luaReturn Player?");
        assertEquals(1, docs.returns().size());
        assertEquals("Player?", docs.returns().get(0).typeExpr());
        assertEquals("", docs.returns().get(0).description());
    }

    @Test
    void multipleReturnTagsBecomeMultiReturn() {
        var raw = """
            @luaReturn Player
            @luaReturn World
            """;
        var docs = JavadocTagParser.parse(raw);
        assertEquals(2, docs.returns().size());
        assertEquals("Player", docs.returns().get(0).typeExpr());
        assertEquals("World", docs.returns().get(1).typeExpr());
    }

    @Test
    void paramTagWithDescription() {
        var docs = JavadocTagParser.parse("@luaParam name string - the player's display name");
        var p = docs.params().get(0);
        assertEquals("name", p.name());
        assertEquals("string", p.typeExpr());
        assertEquals("the player's display name", p.description());
    }

    @Test
    void returnTagWithDescription() {
        var docs = JavadocTagParser.parse("@luaReturn Player? - nil if no player matched");
        assertEquals("Player?", docs.returns().get(0).typeExpr());
        assertEquals("nil if no player matched", docs.returns().get(0).description());
    }

    @Test
    void genericTagWithDescription() {
        var docs = JavadocTagParser.parse("@luaGeneric T - the wrapped value type");
        var g = docs.generics().get(0);
        assertEquals("T", g.name());
        assertFalse(g.pack());
        assertEquals("the wrapped value type", g.description());
    }

    @Test
    void functionTypeArrowDoesNotConfuseDescriptionSplit() {
        var docs = JavadocTagParser.parse("@luaParam fn (x: number) -> string - a transform");
        var p = docs.params().get(0);
        assertEquals("(x: number) -> string", p.typeExpr());
        assertEquals("a transform", p.description());
    }

    @Test
    void genericScalar() {
        var docs = JavadocTagParser.parse("@luaGeneric T");
        assertEquals(1, docs.generics().size());
        var g = docs.generics().get(0);
        assertEquals("T", g.name());
        assertFalse(g.pack());
    }

    @Test
    void genericPack() {
        var docs = JavadocTagParser.parse("@luaGeneric T...");
        var g = docs.generics().get(0);
        assertEquals("T", g.name());
        assertTrue(g.pack());
    }

    @Test
    void mixedDescriptionAndTags() {
        var raw = """
            Find a player by name.
            
            Returns nil if no player matches.
            
            @luaGeneric T
            @luaParam name string
            @luaReturn Player?
            """;
        var docs = JavadocTagParser.parse(raw);
        assertEquals("Find a player by name.\n\nReturns nil if no player matches.", docs.description());
        assertEquals(1, docs.generics().size());
        assertEquals(1, docs.params().size());
        assertEquals(1, docs.returns().size());
        assertTrue(docs.diagnostics().isEmpty());
    }

    @Test
    void unknownAtTagsAreDroppedSilently() {
        var raw = """
            Description.
            
            @param ignoredJavadoc see other docs
            @return also ignored
            @since 1.0
            @luaParam name string
            """;
        var docs = JavadocTagParser.parse(raw);
        assertEquals("Description.", docs.description());
        assertEquals(1, docs.params().size());
        assertTrue(docs.diagnostics().isEmpty());
    }

    @Test
    void malformedLuaTagRecordedAsDiagnostic() {
        var docs = JavadocTagParser.parse("@luaParam");
        assertTrue(docs.params().isEmpty());
        assertEquals(1, docs.diagnostics().size());
        assertTrue(docs.diagnostics().getFirst().message().contains("Malformed"));
    }

    @Test
    void unknownLuaTagRecordedAsDiagnostic() {
        var docs = JavadocTagParser.parse("@luaWidget custom");
        assertEquals(1, docs.diagnostics().size());
    }

    @Test
    void leadingAndTrailingBlankDescriptionLinesTrimmed() {
        var raw = "\n\nHello.\n\n@luaReturn nil\n";
        var docs = JavadocTagParser.parse(raw);
        assertEquals("Hello.", docs.description());
    }

    @Test
    void indentedCodeBlockInDescriptionPreserved() {
        var raw = """
            Example:
            
                local x = foo()
            
            @luaReturn nil
            """;
        var docs = JavadocTagParser.parse(raw);
        assertTrue(docs.description().contains("    local x = foo()"));
    }

    // ---------- backtick-wrapped type expressions ----------
    // IntelliJ's Javadoc formatter helpfully inserts spaces inside `<…>` and `…/…`, mangling
    // type expressions like `EventSource<@mapmaker/player.Player>`. Wrapping the type in
    // single backticks tells the IDE to treat it as inline code and leave it alone; the
    // parser strips the backticks before handing the expression downstream.

    @Test
    void returnTagBacktickedWrappedType() {
        var docs = JavadocTagParser.parse("@luaReturn `EventSource<@mapmaker/player.Player>`");
        assertEquals(1, docs.returns().size());
        assertEquals("EventSource<@mapmaker/player.Player>", docs.returns().get(0).typeExpr());
        assertEquals("", docs.returns().get(0).description());
        assertTrue(docs.diagnostics().isEmpty());
    }

    @Test
    void returnTagBacktickedWrappedTypeWithDescription() {
        var docs = JavadocTagParser.parse(
            "@luaReturn `EventSource<@mapmaker/player.Player>` - fires on hit");
        assertEquals(1, docs.returns().size());
        assertEquals("EventSource<@mapmaker/player.Player>", docs.returns().get(0).typeExpr());
        assertEquals("fires on hit", docs.returns().get(0).description());
    }

    @Test
    void paramTagBacktickedWrappedType() {
        var docs = JavadocTagParser.parse("@luaParam init `$Writable<TextProp> & { x: number }`");
        assertEquals(1, docs.params().size());
        var p = docs.params().get(0);
        assertEquals("init", p.name());
        assertFalse(p.optional());
        assertEquals("$Writable<TextProp> & { x: number }", p.typeExpr());
        assertEquals("", p.description());
    }

    @Test
    void paramTagBacktickedWrappedTypeWithDescription() {
        var docs = JavadocTagParser.parse(
            "@luaParam init `$Writable<TextProp>` - initial property table");
        assertEquals(1, docs.params().size());
        var p = docs.params().get(0);
        assertEquals("init", p.name());
        assertEquals("$Writable<TextProp>", p.typeExpr());
        assertEquals("initial property table", p.description());
    }

    @Test
    void backtickedTypePreservesSlashes() {
        // The exact case the user hit: IntelliJ would have inserted spaces inside the angle
        // brackets without the backtick wrapper. With backticks, the spaces never appear and
        // the parser sees the canonical form.
        var docs = JavadocTagParser.parse("@luaReturn `EventSource<@mapmaker/player.Player>`");
        assertEquals("EventSource<@mapmaker/player.Player>",
            docs.returns().get(0).typeExpr());
    }

    @Test
    void unterminatedBacktickFallsThroughToSplitter() {
        // An opening backtick with no closing one is malformed. We don't throw — the type
        // parser downstream will produce a clearer error pinned to the offending element —
        // but the typeExpr returned should still contain the original characters (minus the
        // splitter) so the downstream error message names what the author actually wrote.
        var docs = JavadocTagParser.parse("@luaReturn `unclosed");
        assertEquals(1, docs.returns().size());
        assertEquals("`unclosed", docs.returns().get(0).typeExpr());
    }
}

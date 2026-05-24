package net.hollowcube.scripting.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LuauTypeParserTest {

    // ===================== Primitives =====================

    @ParameterizedTest
    @ValueSource(strings = {"string", "number", "boolean", "nil", "unknown", "any", "never", "thread", "buffer", "vector"})
    void primitives(String name) {
        var ty = LuauTypeParser.parse(name);
        var named = assertInstanceOf(LuauType.Named.class, ty);
        assertNull(named.module());
        assertEquals(name, named.name());
        assertTrue(named.args().isEmpty());
    }

    // ===================== Singletons =====================

    @Test
    void stringLiteralDouble() {
        var ty = LuauTypeParser.parse("\"create\"");
        assertEquals(new LuauType.StringLiteral("create"), ty);
    }

    @Test
    void stringLiteralSingle() {
        var ty = LuauTypeParser.parse("'destroy'");
        assertEquals(new LuauType.StringLiteral("destroy"), ty);
    }

    @Test
    void boolLiteralTrue() {
        assertEquals(new LuauType.BoolLiteral(true), LuauTypeParser.parse("true"));
    }

    @Test
    void boolLiteralFalse() {
        assertEquals(new LuauType.BoolLiteral(false), LuauTypeParser.parse("false"));
    }

    // ===================== Optional / Union / Intersection =====================

    @Test
    void optionalSuffix() {
        var ty = LuauTypeParser.parse("Player?");
        var opt = assertInstanceOf(LuauType.Optional.class, ty);
        var inner = assertInstanceOf(LuauType.Named.class, opt.inner());
        assertEquals("Player", inner.name());
    }

    @Test
    void union() {
        var ty = LuauTypeParser.parse("string | number");
        var u = assertInstanceOf(LuauType.Union.class, ty);
        assertEquals(2, u.alternatives().size());
    }

    @Test
    void unionWithThree() {
        var ty = LuauTypeParser.parse("\"a\" | \"b\" | \"c\"");
        var u = assertInstanceOf(LuauType.Union.class, ty);
        assertEquals(3, u.alternatives().size());
    }

    @Test
    void intersection() {
        var ty = LuauTypeParser.parse("Foo & Bar");
        var i = assertInstanceOf(LuauType.Intersection.class, ty);
        assertEquals(2, i.conjuncts().size());
    }

    @Test
    void optionalAppliesBeforeUnion() {
        // "T? | U" parses as Union[Optional[T], U]
        var ty = LuauTypeParser.parse("T? | U");
        var u = assertInstanceOf(LuauType.Union.class, ty);
        assertInstanceOf(LuauType.Optional.class, u.alternatives().get(0));
        assertInstanceOf(LuauType.Named.class, u.alternatives().get(1));
    }

    // ===================== Named / Qualified =====================

    @Test
    void qualifiedName() {
        var ty = LuauTypeParser.parse("players.Player");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals("players", n.module());
        assertEquals("Player", n.name());
    }

    @Test
    void fullyQualifiedModuleReference() {
        var ty = LuauTypeParser.parse("@mapmaker/players.Player");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals("@mapmaker/players", n.module());
        assertEquals("Player", n.name());
    }

    @Test
    void fullyQualifiedWithGenerics() {
        var ty = LuauTypeParser.parse("@mapmaker/players.List<Player>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals("@mapmaker/players", n.module());
        assertEquals(1, n.args().size());
    }

    @Test
    void genericInstantiationSingleArg() {
        var ty = LuauTypeParser.parse("List<Player>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals(1, n.args().size());
        var single = assertInstanceOf(LuauType.TypeArg.Single.class, n.args().get(0));
        assertInstanceOf(LuauType.Named.class, single.type());
    }

    @Test
    void genericInstantiationMultipleArgs() {
        var ty = LuauTypeParser.parse("Map<string, number>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals(2, n.args().size());
    }

    @Test
    void genericPackArg() {
        // EventSource<(Player)> — a single-element type pack.
        var ty = LuauTypeParser.parse("EventSource<(Player)>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        var pack = assertInstanceOf(LuauType.TypeArg.Pack.class, n.args().get(0));
        assertEquals(1, pack.types().size());
        assertNull(pack.tail());
    }

    @Test
    void genericPackArgWithMultipleTypes() {
        var ty = LuauTypeParser.parse("EventSource<(Player, World)>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        var pack = assertInstanceOf(LuauType.TypeArg.Pack.class, n.args().get(0));
        assertEquals(2, pack.types().size());
    }

    @Test
    void genericPackArgWithTail() {
        var ty = LuauTypeParser.parse("EventSource<(Player, ...World)>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        var pack = assertInstanceOf(LuauType.TypeArg.Pack.class, n.args().get(0));
        assertEquals(1, pack.types().size());
        assertNotNull(pack.tail());
    }

    @Test
    void genericPackByName() {
        // Foo<T...> — generic-pack reference
        var ty = LuauTypeParser.parse("Foo<T...>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        var pack = assertInstanceOf(LuauType.TypeArg.Pack.class, n.args().get(0));
        assertNotNull(pack.tail());
        var gp = assertInstanceOf(LuauType.GenericPack.class, pack.tail().element());
        assertEquals("T", gp.name());
    }

    // ===================== Function =====================

    @Test
    void functionNoParamsNoReturn() {
        var ty = LuauTypeParser.parse("() -> ()");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertTrue(f.params().isEmpty());
        assertNull(f.varargs());
        assertTrue(f.returns().isEmpty());
    }

    @Test
    void functionSingleParamSingleReturn() {
        var ty = LuauTypeParser.parse("(Player) -> string");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals(1, f.params().size());
        assertNull(f.params().get(0).name());
        assertEquals(1, f.returns().size());
    }

    @Test
    void functionNamedParam() {
        var ty = LuauTypeParser.parse("(name: string) -> nil");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals("name", f.params().get(0).name());
    }

    @Test
    void functionOptionalNamedParam() {
        var ty = LuauTypeParser.parse("(name?: string) -> nil");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals("name", f.params().get(0).name());
        assertInstanceOf(LuauType.Optional.class, f.params().get(0).type());
    }

    @Test
    void functionMultipleParams() {
        var ty = LuauTypeParser.parse("(string, number) -> ()");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals(2, f.params().size());
    }

    @Test
    void functionMultiReturn() {
        var ty = LuauTypeParser.parse("(Player) -> (string, number)");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals(2, f.returns().size());
    }

    @Test
    void functionVariadicParams() {
        var ty = LuauTypeParser.parse("(...number) -> ()");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertTrue(f.params().isEmpty());
        assertNotNull(f.varargs());
        var elem = assertInstanceOf(LuauType.Named.class, f.varargs().element());
        assertEquals("number", elem.name());
    }

    @Test
    void functionReturningFunction() {
        var ty = LuauTypeParser.parse("(Player) -> (Player) -> string");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals(1, f.returns().size());
        assertInstanceOf(LuauType.Function.class, f.returns().get(0));
    }

    // ===================== Tables =====================

    @Test
    void tableArrayForm() {
        var ty = LuauTypeParser.parse("{Player}");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertEquals(0, t.fields().size());
        assertNotNull(t.arrayElement());
        assertNull(t.indexerKey());
    }

    @Test
    void tableEmpty() {
        var ty = LuauTypeParser.parse("{}");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertEquals(0, t.fields().size());
        assertNull(t.arrayElement());
    }

    @Test
    void tableRecord() {
        var ty = LuauTypeParser.parse("{ name: string, age: number }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertEquals(2, t.fields().size());
        assertEquals("name", t.fields().get(0).name());
        assertEquals("age", t.fields().get(1).name());
    }

    @Test
    void tableOptionalField() {
        var ty = LuauTypeParser.parse("{ name?: string }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertInstanceOf(LuauType.Optional.class, t.fields().get(0).type());
    }

    @Test
    void tableIndexer() {
        var ty = LuauTypeParser.parse("{ [string]: number }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertNotNull(t.indexerKey());
        assertNotNull(t.indexerValue());
    }

    @Test
    void tableMixedFieldsAndIndexer() {
        var ty = LuauTypeParser.parse("{ tag: string, [number]: Player }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertEquals(1, t.fields().size());
        assertNotNull(t.indexerKey());
    }

    @Test
    void tableSemicolonSeparator() {
        var ty = LuauTypeParser.parse("{ a: number; b: string }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertEquals(2, t.fields().size());
    }

    // ===================== typeof =====================

    @Test
    void typeofSimple() {
        var ty = LuauTypeParser.parse("typeof(foo)");
        var t = assertInstanceOf(LuauType.TypeOf.class, ty);
        assertTrue(t.expr().contains("foo"));
    }

    // ===================== Errors =====================

    @Test
    void emptyInputFails() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse(""));
    }

    @Test
    void unterminatedStringFails() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("\"unfinished"));
    }

    @Test
    void unterminatedAngleFails() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("Foo<Bar"));
    }

    @Test
    void unterminatedTableFails() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("{ a: number"));
    }

    @Test
    void readModifierRejected() {
        var ex = assertThrows(LuauParseException.class,
            () -> LuauTypeParser.parse("{ read x: number }"));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void writeModifierRejected() {
        assertThrows(LuauParseException.class,
            () -> LuauTypeParser.parse("{ write y: string }"));
    }

    @Test
    void variadicAtTopLevelRejected() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("...number"));
    }

    @Test
    void inlineGenericPrefixRejected() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("<T>(T) -> T"));
    }

    @Test
    void trailingTokensRejected() {
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("string number"));
    }

    @Test
    void offsetRecordedOnError() {
        try {
            LuauTypeParser.parse("Foo<Bar");
            fail("expected parse to fail");
        } catch (LuauParseException ex) {
            assertTrue(ex.offset() > 0);
        }
    }

    // ===================== Combinations =====================

    @Test
    void tableOfFunctions() {
        var ty = LuauTypeParser.parse("{ on: (string) -> () }");
        var t = assertInstanceOf(LuauType.Table.class, ty);
        assertInstanceOf(LuauType.Function.class, t.fields().get(0).type());
    }

    @Test
    void unionOfSingletons() {
        var ty = LuauTypeParser.parse("\"create\" | \"destroy\"");
        var u = assertInstanceOf(LuauType.Union.class, ty);
        assertInstanceOf(LuauType.StringLiteral.class, u.alternatives().get(0));
        assertInstanceOf(LuauType.StringLiteral.class, u.alternatives().get(1));
    }

    @Test
    void nestedGenerics() {
        var ty = LuauTypeParser.parse("List<Map<string, Player>>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        var inner = assertInstanceOf(LuauType.TypeArg.Single.class, n.args().get(0));
        assertInstanceOf(LuauType.Named.class, inner.type());
    }

    @Test
    void eventSourceFromUserExample() {
        // Real-world example from the spec: LibPlayers.onJoin returns EventSource<(Player)>.
        var ty = LuauTypeParser.parse("EventSource<(Player)>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertEquals("EventSource", n.name());
        var pack = assertInstanceOf(LuauType.TypeArg.Pack.class, n.args().get(0));
        assertEquals(1, pack.types().size());
    }

    @Test
    void multiReturnEmpty() {
        var ty = LuauTypeParser.parse("() -> ()");
        var f = assertInstanceOf(LuauType.Function.class, ty);
        assertEquals(List.of(), f.returns());
    }

    // ===================== Meta-types =====================

    @Test
    void metaTypeNameLexesAsSingleNamed() {
        var ty = LuauTypeParser.parse("$Writable<Foo>");
        var n = assertInstanceOf(LuauType.Named.class, ty);
        assertNull(n.module());
        assertEquals("$Writable", n.name());
        assertEquals(1, n.args().size());
        var arg = assertInstanceOf(LuauType.TypeArg.Single.class, n.args().getFirst());
        assertEquals("Foo", assertInstanceOf(LuauType.Named.class, arg.type()).name());
    }

    @Test
    void barDollarRejected() {
        // Bare `$` with no name after it is a parse error — meta-types must name something.
        assertThrows(LuauParseException.class, () -> LuauTypeParser.parse("$"));
    }

    @Test
    void metaTypeNestedInside() {
        var ty = LuauTypeParser.parse("Map<string, $Writable<Foo>>");
        var outer = assertInstanceOf(LuauType.Named.class, ty);
        var second = assertInstanceOf(LuauType.TypeArg.Single.class, outer.args().get(1));
        assertEquals("$Writable", assertInstanceOf(LuauType.Named.class, second.type()).name());
    }
}

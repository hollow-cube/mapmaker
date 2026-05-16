package net.hollowcube.luau.engineapi.emit;

import net.hollowcube.luau.slopgen.types.LuauType;
import net.hollowcube.luau.slopgen.types.LuauTypeParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.hollowcube.luau.engineapi.emit.RenderContext.CANONICAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// One assertion per [LuauType] variant plus a parser round-trip invariant
/// (`parse(render(t, CANONICAL)) == t`), since the renderer is the exact inverse of
/// `LuauTypeParser`.
class LuauTypeRendererTest {

    private static String r(LuauType t) {
        return LuauTypeRenderer.render(t, CANONICAL);
    }

    private static LuauType named(String name) {
        return new LuauType.Named(null, name, List.of());
    }

    // ---- per-variant rendering ----

    @Test
    void builtinAndSameFileNamed() {
        assertEquals("number", r(named("number")));
        assertEquals("Player", r(named("Player")));
    }

    @Test
    void crossModuleNamedIsQualified() {
        assertEquals("@mapmaker/world.Entity",
            r(new LuauType.Named("@mapmaker/world", "Entity", List.of())));
    }

    @Test
    void namedWithArgs() {
        assertEquals("Map<string, number>", r(new LuauType.Named(null, "Map", List.of(
            new LuauType.TypeArg.Single(named("string")),
            new LuauType.TypeArg.Single(named("number"))))));
    }

    @Test
    void genericRefAndPack() {
        assertEquals("T", r(new LuauType.GenericRef("T")));
        assertEquals("T...", r(new LuauType.GenericPack("T")));
    }

    @Test
    void optionalParenthesizesWhenNeeded() {
        assertEquals("number?", r(new LuauType.Optional(named("number"))));
        assertEquals("(A | B)?", r(new LuauType.Optional(
            new LuauType.Union(List.of(named("A"), named("B"))))));
        assertEquals("((A) -> B)?", r(new LuauType.Optional(
            new LuauType.Function(List.of(new LuauType.Param(null, named("A"))), null,
                List.of(named("B"))))));
    }

    @Test
    void unionAndIntersection() {
        assertEquals("A | B | C", r(new LuauType.Union(List.of(named("A"), named("B"), named("C")))));
        assertEquals("A & B", r(new LuauType.Intersection(List.of(named("A"), named("B")))));
        // a function alternative must be grouped
        assertEquals("((A) -> B) | nil", r(new LuauType.Union(List.of(
            new LuauType.Function(List.of(new LuauType.Param(null, named("A"))), null, List.of(named("B"))),
            named("nil")))));
    }

    @Test
    void tables() {
        assertEquals("{string}", r(new LuauType.Table(List.of(), named("string"), null, null)));
        assertEquals("{}", r(new LuauType.Table(List.of(), null, null, null)));
        assertEquals("{ a: number, b: string }", r(new LuauType.Table(List.of(
            new LuauType.TableField("a", named("number")),
            new LuauType.TableField("b", named("string"))), null, null, null)));
        assertEquals("{ [string]: number }",
            r(new LuauType.Table(List.of(), null, named("string"), named("number"))));
        assertEquals("{ a: number, [string]: any }", r(new LuauType.Table(
            List.of(new LuauType.TableField("a", named("number"))), null,
            named("string"), named("any"))));
    }

    @Test
    void functions() {
        assertEquals("() -> ()", r(new LuauType.Function(List.of(), null, List.of())));
        assertEquals("(x: number) -> string", r(new LuauType.Function(
            List.of(new LuauType.Param("x", named("number"))), null, List.of(named("string")))));
        assertEquals("(...number) -> ()", r(new LuauType.Function(
            List.of(), new LuauType.Variadic(named("number")), List.of())));
        assertEquals("(A) -> (B, C)", r(new LuauType.Function(
            List.of(new LuauType.Param(null, named("A"))), null, List.of(named("B"), named("C")))));
        // single function/pack return is wrapped
        assertEquals("() -> ((A) -> B)", r(new LuauType.Function(List.of(), null, List.of(
            new LuauType.Function(List.of(new LuauType.Param(null, named("A"))), null, List.of(named("B")))))));
        assertEquals("() -> (T...)", r(new LuauType.Function(List.of(), null,
            List.of(new LuauType.GenericPack("T")))));
    }

    @Test
    void literalsAndTypeof() {
        assertEquals("\"create\"", r(new LuauType.StringLiteral("create")));
        assertEquals("\"a\\nb\"", r(new LuauType.StringLiteral("a\nb")));
        assertEquals("true", r(new LuauType.BoolLiteral(true)));
        assertEquals("typeof(x.y)", r(new LuauType.TypeOf("x.y")));
    }

    @Test
    void typeArgPacks() {
        assertEquals("Foo<(A, B)>", r(new LuauType.Named(null, "Foo", List.of(
            new LuauType.TypeArg.Pack(List.of(named("A"), named("B")), null)))));
        assertEquals("Foo<(A, ...B)>", r(new LuauType.Named(null, "Foo", List.of(
            new LuauType.TypeArg.Pack(List.of(named("A")), new LuauType.Variadic(named("B")))))));
        assertEquals("Foo<()>", r(new LuauType.Named(null, "Foo", List.of(
            new LuauType.TypeArg.Pack(List.of(), null)))));
        assertEquals("Foo<T...>", r(new LuauType.Named(null, "Foo", List.of(
            new LuauType.TypeArg.Pack(List.of(), new LuauType.Variadic(new LuauType.GenericPack("T")))))));
    }

    // ---- round-trip: parse(render(t)) == t ----

    @Test
    void roundTrips() {
        List<LuauType> samples = List.of(
            named("string"),
            new LuauType.Named("@mapmaker/world", "Entity", List.of()),
            new LuauType.Named(null, "Map", List.of(
                new LuauType.TypeArg.Single(named("string")),
                new LuauType.TypeArg.Single(named("number")))),
            new LuauType.Optional(named("number")),
            new LuauType.Optional(new LuauType.Union(List.of(named("A"), named("B")))),
            new LuauType.Union(List.of(named("A"), named("B"), named("nil"))),
            new LuauType.Intersection(List.of(named("A"),
                new LuauType.Table(List.of(new LuauType.TableField("x", named("number"))), null, null, null))),
            new LuauType.Table(List.of(), named("string"), null, null),
            new LuauType.Table(List.of(), null, named("string"), named("number")),
            new LuauType.Function(
                List.of(new LuauType.Param("a", named("number")), new LuauType.Param(null, named("string"))),
                new LuauType.Variadic(named("any")),
                List.of(named("boolean"))),
            new LuauType.Function(List.of(), null, List.of(named("A"), named("B"))),
            new LuauType.Function(List.of(new LuauType.Param(null, new LuauType.GenericPack("A"))), null,
                List.of(new LuauType.GenericPack("R"))),
            new LuauType.StringLiteral("a\tb"),
            new LuauType.BoolLiteral(false));

        for (var t : samples) {
            String rendered = r(t);
            LuauType reparsed = LuauTypeParser.parse(rendered);
            assertEquals(normalize(t), normalize(reparsed),
                "round-trip mismatch for: " + rendered);
        }
    }

    /// The parser never produces `GenericRef` (the resolver does); treat it as the bare
    /// `Named(null, name, [])` it parses back to.
    private static LuauType normalize(LuauType t) {
        return switch (t) {
            case LuauType.GenericRef g -> new LuauType.Named(null, g.name(), List.of());
            case LuauType.Optional o -> new LuauType.Optional(normalize(o.inner()));
            case LuauType.Union u ->
                new LuauType.Union(u.alternatives().stream().map(LuauTypeRendererTest::normalize).toList());
            case LuauType.Intersection i ->
                new LuauType.Intersection(i.conjuncts().stream().map(LuauTypeRendererTest::normalize).toList());
            case LuauType.Table tb -> new LuauType.Table(
                tb.fields().stream().map(f -> new LuauType.TableField(f.name(), normalize(f.type()))).toList(),
                tb.arrayElement() == null ? null : normalize(tb.arrayElement()),
                tb.indexerKey() == null ? null : normalize(tb.indexerKey()),
                tb.indexerValue() == null ? null : normalize(tb.indexerValue()));
            case LuauType.Function f -> new LuauType.Function(
                f.params().stream().map(p -> new LuauType.Param(p.name(), normalize(p.type()))).toList(),
                f.varargs() == null ? null : new LuauType.Variadic(normalize(f.varargs().element())),
                f.returns().stream().map(LuauTypeRendererTest::normalize).toList());
            default -> t;
        };
    }
}

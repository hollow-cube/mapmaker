package net.hollowcube.luau.docs.resolve;

import net.hollowcube.luau.gen.docs.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrossModuleResolverTest {

    @Test
    void primitiveResolves() {
        var lib = libraryWithStaticMethod("@t/p", "noop",
            List.of(new RawParam("x", false, "string")),
            List.of("number"));
        var diags = resolve(List.of(lib));
        assertTrue(diags.isEmpty(), () -> diags.toString());
    }

    @Test
    void bareSiblingExportResolves() {
        var lib = libraryWithExportAndMethod(
            "@t/sib", "Player",
            "find",
            List.of(),
            List.of("Player?"));
        var diags = resolve(List.of(lib));
        assertTrue(diags.isEmpty(), () -> diags.toString());
    }

    @Test
    void unresolvedBareNameFails() {
        var lib = libraryWithStaticMethod("@t/u", "find",
            List.of(),
            List.of("Goblin"));
        var diags = resolve(List.of(lib));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("unresolved type 'Goblin'"));
    }

    @Test
    void fullyQualifiedCrossLibraryResolves() {
        var libA = libraryWithExportAndMethod("@t/a", "Player", "noop", List.of(), List.of("nil"));
        var libB = libraryWithStaticMethod("@t/b", "find",
            List.of(),
            List.of("@t/a.Player"));
        var diags = resolve(List.of(libA, libB));
        assertTrue(diags.isEmpty(), () -> diags.toString());
    }

    @Test
    void unresolvedFullyQualifiedReferenceFails() {
        var lib = libraryWithStaticMethod("@t/x", "find",
            List.of(),
            List.of("@nope/lib.Goblin"));
        var diags = resolve(List.of(lib));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("unresolved cross-library"));
    }

    @Test
    void shortFormQualifierRejected() {
        var libA = libraryWithExportAndMethod("@t/a", "Player", "noop", List.of(), List.of("nil"));
        var libB = libraryWithStaticMethod("@t/b", "find",
            List.of(),
            List.of("a.Player"));
        var diags = resolve(List.of(libA, libB));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("short-form module qualifier"));
    }

    @Test
    void inScopeGenericResolves() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/g", "REQUIRE", "fixtures.LibG", "",
            List.of(new RawMethod("identity", "identity", "",
                List.of(new RawGeneric("T", false)),
                List.of(new RawParam("x", false, "T")),
                List.of("T"))),
            List.of(),
            List.of());
        var diags = resolve(List.of(lib));
        assertTrue(diags.isEmpty(), () -> diags.toString());
    }

    @Test
    void unboundGenericFails() {
        var lib = libraryWithStaticMethod("@t/ug", "noop",
            List.of(new RawParam("x", false, "U")),
            List.of("nil"));
        var diags = resolve(List.of(lib));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("unresolved type 'U'"));
    }

    @Test
    void genericPackResolves() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/gp", "REQUIRE", "fixtures.LibGp", "",
            List.of(new RawMethod("noop", "noop", "",
                List.of(new RawGeneric("Args", true)),
                List.of(),
                List.of("EventSource<Args...>"))),
            List.of(),
            List.of(new RawExport("EventSource", "fixtures.LibGp.EventSource", "", null,
                true, List.of(), List.of(), List.of())));
        var diags = resolve(List.of(lib));
        assertTrue(diags.isEmpty(), () -> diags.toString());
    }

    @Test
    void packAsScalarFails() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/ps", "REQUIRE", "fixtures.LibPs", "",
            List.of(new RawMethod("noop", "noop", "",
                List.of(new RawGeneric("T", true)),  // T is a pack
                List.of(new RawParam("x", false, "T")),  // …but used as scalar
                List.of("nil"))),
            List.of(),
            List.of());
        var diags = resolve(List.of(lib));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("used in scalar position"));
    }

    @Test
    void scalarAsPackFails() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/sp", "REQUIRE", "fixtures.LibSp", "",
            List.of(new RawMethod("noop", "noop", "",
                List.of(new RawGeneric("T", false)),  // T is a scalar
                List.of(),
                List.of("EventSource<T...>"))),  // …but used as a pack
            List.of(),
            List.of(new RawExport("EventSource", "fixtures.LibSp.EventSource", "", null,
                true, List.of(), List.of(), List.of())));
        var diags = resolve(List.of(lib));
        assertFalse(diags.isEmpty());
        assertTrue(diags.stream().anyMatch(d -> d.message().contains("declared as a scalar")));
    }

    @Test
    void getterTypeExpressionResolved() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/gt", "REQUIRE", "fixtures.LibGt", "",
            List.of(),
            List.of(new RawProperty("count",
                new RawGetter("getCount", "", "Goblin"),
                null)),
            List.of());
        var diags = resolve(List.of(lib));
        assertEquals(1, diags.size());
        assertTrue(diags.get(0).message().contains("'Goblin'"));
    }

    @Test
    void unresolvedSuperExportFlags() {
        var lib = new RawLibrary(
            1, "raw-library", "@t/super", "REQUIRE", "fixtures.LibSuper", "",
            List.of(),
            List.of(),
            List.of(new RawExport("Pup", "fixtures.LibSuper.Pup",
                "", "fixtures.unknown.Mystery", true,
                List.of(), List.of(), List.of())));
        var diags = resolve(List.of(lib));
        assertFalse(diags.isEmpty());
        assertTrue(diags.stream().anyMatch(d -> d.message().contains("superExport")));
    }

    @Test
    void nestedTypeInsideGenericResolved() {
        var libA = libraryWithExport("@t/n1", "Player");
        var libB = libraryWithStaticMethod("@t/n2", "find", List.of(),
            List.of("List<@t/n1.Player>"));
        var diags = resolve(List.of(libA, libB));
        assertTrue(diags.stream().anyMatch(d -> d.message().contains("'List'")),
            "'List' should be flagged as unresolved");
        // @t/n1.Player should resolve OK; only 'List' is missing.
        assertEquals(1, diags.size(), () -> diags.toString());
    }

    // ----- helpers -----

    private static List<ResolveDiagnostic> resolve(List<RawLibrary> libs) {
        var symbols = SymbolTable.build(libs);
        var out = new ArrayList<ResolveDiagnostic>();
        for (var lib : libs) CrossModuleResolver.resolve(lib, symbols, out);
        return out;
    }

    private static RawLibrary libraryWithStaticMethod(String mod, String name,
                                                      List<RawParam> params, List<String> returns) {
        return new RawLibrary(
            1, "raw-library", mod, "REQUIRE", "fixtures." + mod.replace("@", "").replace("/", "_"),
            "",
            List.of(new RawMethod(name, name, "", List.of(), params, returns)),
            List.of(),
            List.of());
    }

    private static RawLibrary libraryWithExport(String mod, String exportLuaName) {
        return new RawLibrary(
            1, "raw-library", mod, "REQUIRE", "fixtures." + exportLuaName + "Lib", "",
            List.of(),
            List.of(),
            List.of(new RawExport(exportLuaName,
                "fixtures." + exportLuaName + "Lib." + exportLuaName,
                "", null, true, List.of(), List.of(), List.of())));
    }

    private static RawLibrary libraryWithExportAndMethod(String mod, String exportLuaName,
                                                         String methodName, List<RawParam> params,
                                                         List<String> returns) {
        return new RawLibrary(
            1, "raw-library", mod, "REQUIRE", "fixtures." + exportLuaName + "Lib", "",
            List.of(new RawMethod(methodName, methodName, "", List.of(), params, returns)),
            List.of(),
            List.of(new RawExport(exportLuaName,
                "fixtures." + exportLuaName + "Lib." + exportLuaName,
                "", null, true, List.of(), List.of(), List.of())));
    }
}

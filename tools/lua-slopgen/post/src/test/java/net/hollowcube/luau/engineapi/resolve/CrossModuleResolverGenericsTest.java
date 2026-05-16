package net.hollowcube.luau.engineapi.resolve;

import com.palantir.javapoet.ClassName;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.types.LuauType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Resolver-level coverage for type-level generics: a `@luaGeneric` declared on the
/// `@LuaExport` is in scope for every method, accessor, and meta-method, and only those
/// names — typos still surface as unresolved-generic-pack diagnostics.
class CrossModuleResolverGenericsTest {

    @Test
    void typeLevelPackIsInScopeAcrossMembers() {
        // EventSource<A...> with three methods that each reference A. A method-level scope
        // is empty; the resolver merges in the type-level pack.
        var typeAPack = new LuauType.GenericPack("A");

        var listenParam = new Model.Param("handler",
            /*optional=*/false,
            new LuauType.Function(
                List.of(),
                /*varargs=*/null,
                List.of(typeAPack)),
            "");
        var listen = new Model.Method(
            "listen", "listen", /*isVoid=*/false,
            ClassName.get("fixtures", "Lib", "EventSource"),
            "", List.of(), List.of(listenParam), List.of());

        var waitMethod = new Model.Method(
            "wait", "wait", /*isVoid=*/false,
            ClassName.get("fixtures", "Lib", "EventSource"),
            "", List.of(), List.of(),
            List.of(new Model.Return(typeAPack, "")));

        var export = new Model.Export(
            ClassName.get("fixtures", "Lib", "EventSource"),
            "EventSource",
            /*superExport=*/null, /*isFinal=*/true,
            List.of(new Model.GenericParam("A", /*pack=*/true, "")),
            List.of(),
            List.of(listen, waitMethod),
            List.of(),
            /*userDataTag=*/1, /*hasSubtypes=*/false, "");

        var diagnostics = runResolver(export);
        assertTrue(diagnostics.isEmpty(),
            "no diagnostics expected — A is bound by the type-level @luaGeneric.\n" + diagnostics);
    }

    @Test
    void typeLevelScalarIsInScopeOnAccessors() {
        var typeT = new LuauType.Named(null, "T", List.of());

        var getter = new Model.Accessor("getValue",
            ClassName.get("fixtures", "Lib", "Box"),
            "", null, typeT);
        var setter = new Model.Accessor("setValue",
            ClassName.get("fixtures", "Lib", "Box"),
            "", "value", typeT);
        var prop = new Model.Property("value", getter, setter);

        var export = new Model.Export(
            ClassName.get("fixtures", "Lib", "Box"),
            "Box",
            null, true,
            List.of(new Model.GenericParam("T", /*pack=*/false, "")),
            List.of(prop), List.of(), List.of(),
            1, false, "");

        var diagnostics = runResolver(export);
        assertTrue(diagnostics.isEmpty(),
            "type-level T should resolve on both getter and setter.\n" + diagnostics);
    }

    @Test
    void unknownGenericPackStillFlagged() {
        // Type declares A...; a method tries to reference B... which doesn't exist anywhere.
        var bPack = new LuauType.GenericPack("B");

        var method = new Model.Method(
            "drop", "drop", /*isVoid=*/false,
            ClassName.get("fixtures", "Lib", "Source"),
            "", List.of(),
            List.of(new Model.Param("handler", false,
                new LuauType.Function(List.of(), null, List.of(bPack)), "")),
            List.of());

        var export = new Model.Export(
            ClassName.get("fixtures", "Lib", "Source"),
            "Source", null, true,
            List.of(new Model.GenericParam("A", true, "")),
            List.of(), List.of(method), List.of(),
            1, false, "");

        var diagnostics = runResolver(export);
        assertEquals(1, diagnostics.size(), diagnostics.toString());
        assertTrue(diagnostics.get(0).message().contains("B..."),
            "expected the diagnostic to mention the unresolved pack 'B...'.\n" + diagnostics);
    }

    @Test
    void methodLocalGenericComposesWithTypeLevel() {
        // Type: A... ; method: R (scalar). Both names resolve in `(A...) -> R`.
        var typeAPack = new LuauType.GenericPack("A");
        var rRef = new LuauType.Named(null, "R", List.of());

        var fnType = new LuauType.Function(List.of(), null, List.of(rRef));
        var collect = new Model.Method(
            "collect", "collect", false,
            ClassName.get("fixtures", "Lib", "Source"),
            "",
            List.of(new Model.GenericParam("R", false, "")),
            List.of(new Model.Param("map", false,
                new LuauType.Function(
                    List.of(new LuauType.Param(null, typeAPack)),
                    null,
                    List.of(rRef)),
                "")),
            List.of(new Model.Return(rRef, "")));
        // (Drop unused fnType — keep just to make the setup intent obvious in failure logs.)
        assertEquals(0, fnType.params().size());

        var export = new Model.Export(
            ClassName.get("fixtures", "Lib", "Source"),
            "Source", null, true,
            List.of(new Model.GenericParam("A", true, "")),
            List.of(), List.of(collect), List.of(),
            1, false, "");

        var diagnostics = runResolver(export);
        assertTrue(diagnostics.isEmpty(),
            "type-level A and method-level R should both resolve.\n" + diagnostics);
    }

    private static List<ResolveDiagnostic> runResolver(Model.Export export) {
        var lib = new Model.Library(
            ClassName.get("fixtures", "Lib"),
            ClassName.get("fixtures", "Lib$luau"),
            "@t/lib", LuaLibrary.Scope.REQUIRE,
            List.of(export), List.of(), List.of(), "");
        var symbols = SymbolTable.build(List.of(lib));
        var diagnostics = new ArrayList<ResolveDiagnostic>();
        CrossModuleResolver.resolve(lib, symbols, diagnostics);
        return diagnostics;
    }
}

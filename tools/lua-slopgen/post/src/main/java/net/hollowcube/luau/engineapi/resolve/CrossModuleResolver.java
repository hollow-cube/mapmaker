package net.hollowcube.luau.engineapi.resolve;

import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.types.LuauBuiltins;
import net.hollowcube.luau.slopgen.types.LuauType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Validates a [Model.Library]: every named type resolves to either an in-scope generic or a
/// known [SymbolEntry], every generic-pack reference matches its declared kind, and short-form
/// module qualifiers (`players.Player` instead of `@mapmaker/players.Player`) are rejected with
/// a clear hint.
///
/// Operates directly on the parsed [LuauType] AST stored on the model — no string re-parsing.
public final class CrossModuleResolver {

    private static final Set<String> BUILT_IN_PRIMITIVES = LuauBuiltins.PRIMITIVES;

    private final SymbolTable symbols;
    private final List<ResolveDiagnostic> diagnostics;

    private CrossModuleResolver(SymbolTable symbols, List<ResolveDiagnostic> diagnostics) {
        this.symbols = symbols;
        this.diagnostics = diagnostics;
    }

    public static void resolve(Model.Library lib, SymbolTable symbols, List<ResolveDiagnostic> out) {
        var r = new CrossModuleResolver(symbols, out);
        r.resolveLibrary(lib);
    }

    private void resolveLibrary(Model.Library lib) {
        for (var m : lib.staticMethods()) {
            resolveMethod(lib, "static:" + m.luaName(), GenericScope.empty(),
                m.generics(), m.params(), m.returns());
        }
        for (var p : lib.staticProperties()) {
            resolveProperty(lib, "static:" + p.luaName(), GenericScope.empty(), p);
        }
        for (var ex : lib.exports()) {
            resolveExport(lib, ex);
        }
    }

    private void resolveExport(Model.Library lib, Model.Export ex) {
        if (ex.superExport() != null) {
            var entry = symbols.lookupByJavaType(ex.superExport().toString());
            if (entry == null) {
                diagnostics.add(new ResolveDiagnostic(
                    lib.moduleName() + ":" + ex.luaName() + ".superExport",
                    "superExport '" + ex.superExport() + "' does not match any known @LuaExport"));
            }
        }
        // Type-level generics are visible to every member of the export.
        var typeScope = collectGenerics(ex.generics(), lib, ex.luaName());
        for (var m : ex.methods()) {
            resolveMethod(lib, ex.luaName() + "." + m.luaName(), typeScope,
                m.generics(), m.params(), m.returns());
        }
        for (var p : ex.properties()) {
            resolveProperty(lib, ex.luaName() + "." + p.luaName(), typeScope, p);
        }
        for (var mm : ex.metaMethods()) {
            resolveMethod(lib, ex.luaName() + "." + mm.javaMethodName(), typeScope,
                mm.generics(), mm.params(), mm.returns());
        }
    }

    private void resolveMethod(Model.Library lib, String pathPrefix,
                               GenericScope outerScope,
                               List<Model.GenericParam> generics,
                               List<Model.Param> params,
                               List<Model.Return> returns) {
        var inScope = mergeGenerics(outerScope, generics, lib, pathPrefix);
        for (int i = 0; i < params.size(); i++) {
            var p = params.get(i);
            walk(p.type(), lib, pathPrefix + ":param[" + i + "]:" + p.name(), inScope);
        }
        for (int i = 0; i < returns.size(); i++) {
            walk(returns.get(i).type(), lib, pathPrefix + ":return[" + i + "]", inScope);
        }
    }

    private void resolveProperty(Model.Library lib, String pathPrefix, GenericScope outerScope, Model.Property p) {
        if (p.getter() != null) {
            walk(p.getter().type(), lib, pathPrefix + ":getter:return", outerScope);
        }
        if (p.setter() != null) {
            walk(p.setter().type(), lib, pathPrefix + ":setter:param:" + p.setter().paramName(),
                outerScope);
        }
    }

    /// Build the scope visible to a single method/accessor — the union of `outerScope`
    /// (typically the enclosing `@LuaExport`'s type-level generics) and `generics` declared
    /// on the member itself. The model builder rejects shadow names earlier, so by the time
    /// we get here the two sets are disjoint.
    private GenericScope mergeGenerics(GenericScope outerScope, List<Model.GenericParam> generics,
                                       Model.Library lib, String pathPrefix) {
        var scalars = new HashSet<>(outerScope.scalars());
        var packs = new HashSet<>(outerScope.packs());
        for (var g : generics) (g.pack() ? packs : scalars).add(g.name());
        for (var s : scalars)
            if (packs.contains(s))
                diagnostics.add(new ResolveDiagnostic(
                    lib.moduleName() + ":" + pathPrefix,
                    "generic '" + s + "' is declared both as a scalar and as a pack"));
        return new GenericScope(Set.copyOf(scalars), Set.copyOf(packs));
    }

    private GenericScope collectGenerics(List<Model.GenericParam> generics, Model.Library lib, String pathPrefix) {
        return mergeGenerics(GenericScope.empty(), generics, lib, pathPrefix);
    }

    private void walk(LuauType node, Model.Library lib, String location, GenericScope scope) {
        switch (node) {
            case LuauType.Named n -> walkNamed(n, lib, location, scope);
            case LuauType.GenericPack gp -> {
                if (!scope.packs().contains(gp.name())) {
                    if (scope.scalars().contains(gp.name())) {
                        diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                            "generic '" + gp.name() + "' is used as a pack '" + gp.name()
                            + "...' but declared as a scalar"));
                    } else {
                        diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                            "generic-pack reference '" + gp.name()
                            + "...' is not declared via @luaGeneric " + gp.name() + "..."));
                    }
                }
            }
            case LuauType.Optional o -> walk(o.inner(), lib, location, scope);
            case LuauType.Union u -> {
                for (var a : u.alternatives()) walk(a, lib, location, scope);
            }
            case LuauType.Intersection i -> {
                for (var c : i.conjuncts()) walk(c, lib, location, scope);
            }
            case LuauType.Table t -> {
                for (var f : t.fields()) walk(f.type(), lib, location, scope);
                if (t.arrayElement() != null) walk(t.arrayElement(), lib, location, scope);
                if (t.indexerKey() != null) walk(t.indexerKey(), lib, location, scope);
                if (t.indexerValue() != null) walk(t.indexerValue(), lib, location, scope);
            }
            case LuauType.Function f -> {
                for (var p : f.params()) walk(p.type(), lib, location, scope);
                if (f.varargs() != null) walk(f.varargs().element(), lib, location, scope);
                for (var r : f.returns()) walk(r, lib, location, scope);
            }
            case LuauType.Variadic v -> walk(v.element(), lib, location, scope);
            case LuauType.GenericRef ignored -> { /* already-resolved variant */ }
            case LuauType.TypeOf ignored -> { /* not validated in v1 */ }
            case LuauType.StringLiteral ignored -> {
            }
            case LuauType.BoolLiteral ignored -> {
            }
        }
    }

    private void walkNamed(LuauType.Named n, Model.Library lib, String location, GenericScope scope) {
        if (n.module() == null && n.args().isEmpty() && scope.scalars().contains(n.name())) {
            return; // generic reference
        }
        if (n.module() == null && n.args().isEmpty() && scope.packs().contains(n.name())) {
            diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                "generic pack '" + n.name() + "...' used in scalar position"));
            return;
        }
        if (n.module() == null) {
            if (BUILT_IN_PRIMITIVES.contains(n.name())) {
                // ok
            } else if (symbols.lookupBareInModule(lib.moduleName(), n.name()) == null) {
                diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                    "unresolved type '" + n.name() + "' — declare via @luaGeneric, "
                    + "use a @LuaExport in the same library, or fully-qualify cross-library "
                    + "references like '@mapmaker/<module>." + n.name() + "'"));
            }
        } else if (n.module().startsWith("@")) {
            if (symbols.lookupByKey(n.module(), n.name()) == null) {
                diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                    "unresolved cross-library type '" + n.module() + "." + n.name()
                    + "' — no such @LuaExport"));
            }
        } else {
            diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location,
                "short-form module qualifier '" + n.module() + "." + n.name()
                + "' is not supported. Use bare '" + n.name() + "' for the same library, "
                + "or fully-qualify like '@<group>/" + n.module() + "." + n.name() + "'."));
        }
        for (var arg : n.args()) {
            switch (arg) {
                case LuauType.TypeArg.Single s -> walk(s.type(), lib, location, scope);
                case LuauType.TypeArg.Pack p -> {
                    for (var t : p.types()) walk(t, lib, location, scope);
                    if (p.tail() != null) walk(p.tail().element(), lib, location, scope);
                }
            }
        }
    }

    /// In-scope generics for one method or meta-method.
    public record GenericScope(Set<String> scalars, Set<String> packs) {
        public static GenericScope empty() {
            return new GenericScope(Set.of(), Set.of());
        }
    }
}

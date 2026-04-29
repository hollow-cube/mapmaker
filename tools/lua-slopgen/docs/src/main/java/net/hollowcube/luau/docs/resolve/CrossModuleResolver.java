package net.hollowcube.luau.docs.resolve;

import net.hollowcube.luau.docs.types.LuauParseException;
import net.hollowcube.luau.docs.types.LuauType;
import net.hollowcube.luau.docs.types.LuauTypeParser;
import net.hollowcube.luau.gen.docs.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Validates a [RawLibrary]: every type expression parses, every named type resolves to either
/// an in-scope generic or a known [SymbolEntry], every generic-pack reference matches its
/// declared kind, and short-form module qualifiers (`players.Player` instead of
/// `@mapmaker/players.Player`) are rejected with a clear hint.
public final class CrossModuleResolver {

    private final SymbolTable symbols;
    private final List<ResolveDiagnostic> diagnostics;

    private CrossModuleResolver(SymbolTable symbols, List<ResolveDiagnostic> diagnostics) {
        this.symbols = symbols;
        this.diagnostics = diagnostics;
    }

    public static void resolve(RawLibrary lib, SymbolTable symbols, List<ResolveDiagnostic> out) {
        var r = new CrossModuleResolver(symbols, out);
        r.resolveLibrary(lib);
    }

    private void resolveLibrary(RawLibrary lib) {
        for (var m : lib.staticMethods()) {
            resolveMethod(lib, "static:" + m.luaName(), m);
        }
        for (var p : lib.staticProperties()) {
            resolveProperty(lib, "static:" + p.luaName(), p);
        }
        for (var ex : lib.exports()) {
            resolveExport(lib, ex);
        }
    }

    private void resolveExport(RawLibrary lib, RawExport ex) {
        // Validate superExport: it should be a Java type name we know about (lookup may translate
        // to module-qualified form during emit). Unknown super is a soft warning at this level —
        // the export may extend a non-`@LuaExport` Java parent, which the existing model accepts.
        if (ex.superExport() != null) {
            var entry = symbols.lookupByJavaType(ex.superExport());
            if (entry == null) {
                diagnostics.add(new ResolveDiagnostic(
                    lib.module() + ":" + ex.luaName() + ".superExport",
                    "superExport '" + ex.superExport() + "' does not match any known @LuaExport"));
            }
        }
        for (var m : ex.methods()) {
            resolveMethod(lib, ex.luaName() + "." + m.luaName(), m);
        }
        for (var p : ex.properties()) {
            resolveProperty(lib, ex.luaName() + "." + p.luaName(), p);
        }
        for (var mm : ex.metaMethods()) {
            resolveMetaMethod(lib, ex.luaName() + "." + mm.javaName(), mm);
        }
    }

    private void resolveMethod(RawLibrary lib, String pathPrefix, RawMethod m) {
        var inScope = collectGenerics(m.generics(), lib, pathPrefix);
        for (int i = 0; i < m.params().size(); i++) {
            var param = m.params().get(i);
            resolveTypeExpr(lib, pathPrefix + ":param[" + i + "]:" + param.name(),
                param.typeExpr(), inScope);
        }
        for (int i = 0; i < m.returns().size(); i++) {
            resolveTypeExpr(lib, pathPrefix + ":return[" + i + "]",
                m.returns().get(i), inScope);
        }
    }

    private void resolveMetaMethod(RawLibrary lib, String pathPrefix, RawMetaMethod mm) {
        var inScope = collectGenerics(mm.generics(), lib, pathPrefix);
        for (int i = 0; i < mm.params().size(); i++) {
            var p = mm.params().get(i);
            resolveTypeExpr(lib, pathPrefix + ":param[" + i + "]:" + p.name(),
                p.typeExpr(), inScope);
        }
        for (int i = 0; i < mm.returns().size(); i++) {
            resolveTypeExpr(lib, pathPrefix + ":return[" + i + "]",
                mm.returns().get(i), inScope);
        }
    }

    private void resolveProperty(RawLibrary lib, String pathPrefix, RawProperty p) {
        if (p.getter() != null) {
            resolveGetter(lib, pathPrefix + ":getter", p.getter());
        }
        if (p.setter() != null) {
            resolveSetter(lib, pathPrefix + ":setter", p.setter());
        }
    }

    private void resolveGetter(RawLibrary lib, String pathPrefix, RawGetter g) {
        resolveTypeExpr(lib, pathPrefix + ":return", g.returnTypeExpr(), GenericScope.empty());
    }

    private void resolveSetter(RawLibrary lib, String pathPrefix, RawSetter s) {
        resolveTypeExpr(lib, pathPrefix + ":param:" + s.paramName(),
            s.paramTypeExpr(), GenericScope.empty());
    }

    private GenericScope collectGenerics(List<RawGeneric> generics, RawLibrary lib, String pathPrefix) {
        var scalars = new HashSet<String>();
        var packs = new HashSet<String>();
        for (var g : generics) {
            (g.pack() ? packs : scalars).add(g.name());
        }
        // Detect collisions
        for (var s : scalars)
            if (packs.contains(s))
                diagnostics.add(new ResolveDiagnostic(
                    lib.module() + ":" + pathPrefix,
                    "generic '" + s + "' is declared both as a scalar and as a pack"));
        return new GenericScope(Set.copyOf(scalars), Set.copyOf(packs));
    }

    private void resolveTypeExpr(RawLibrary lib, String location, String src, GenericScope scope) {
        if (src == null || src.isBlank()) {
            diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                "type expression is blank"));
            return;
        }
        LuauType ast;
        try {
            ast = LuauTypeParser.parse(src);
        } catch (LuauParseException ex) {
            diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                "failed to parse '" + src + "': " + ex.getMessage()));
            return;
        }
        walk(ast, lib, location, scope);
    }

    private void walk(LuauType node, RawLibrary lib, String location, GenericScope scope) {
        switch (node) {
            case LuauType.Named n -> walkNamed(n, lib, location, scope);
            case LuauType.GenericPack gp -> {
                if (!scope.packs().contains(gp.name())) {
                    if (scope.scalars().contains(gp.name())) {
                        diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                            "generic '" + gp.name() + "' is used as a pack '" + gp.name()
                            + "...' but declared as a scalar"));
                    } else {
                        diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
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
            case LuauType.GenericRef ignored -> { /* already-resolved variant; nothing to do */ }
            case LuauType.TypeOf ignored -> { /* not validated in v1 */ }
            case LuauType.StringLiteral ignored -> {
            }
            case LuauType.BoolLiteral ignored -> {
            }
        }
    }

    private void walkNamed(LuauType.Named n, RawLibrary lib, String location, GenericScope scope) {
        // Generic ref: bare name matching an in-scope generic.
        if (n.module() == null && n.args().isEmpty() && scope.scalars().contains(n.name())) {
            // OK — generic reference. (We don't mutate the AST here; the resolved JSON
            // emits the source string verbatim. Future work: translate into GenericRef in a
            // resolved-AST output.)
            return;
        }
        if (n.module() == null && n.args().isEmpty() && scope.packs().contains(n.name())) {
            // Used as a scalar but declared as a pack — error.
            diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                "generic pack '" + n.name() + "...' used in scalar position"));
            return;
        }
        if (n.module() == null) {
            // Bare name. Two acceptable resolutions:
            //  1) Built-in primitive (string, number, …) — accepted.
            //  2) Sibling export in the current library — accepted.
            // Otherwise: unresolved reference.
            if (BUILT_IN_PRIMITIVES.contains(n.name())) {
                // ok
            } else if (symbols.lookupBareInModule(lib.module(), n.name()) == null) {
                diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                    "unresolved type '" + n.name() + "' — declare via @luaGeneric, "
                    + "use a @LuaExport in the same library, or fully-qualify cross-library "
                    + "references like '@mapmaker/<module>." + n.name() + "'"));
            }
        } else if (n.module().startsWith("@")) {
            // Fully qualified.
            if (symbols.lookupByKey(n.module(), n.name()) == null) {
                diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                    "unresolved cross-library type '" + n.module() + "." + n.name()
                    + "' — no such @LuaExport"));
            }
        } else {
            // Short-form module qualifier like `players.Player`. Reject — canonical form is
            // bare-in-library or fully-qualified.
            diagnostics.add(new ResolveDiagnostic(lib.module() + ":" + location,
                "short-form module qualifier '" + n.module() + "." + n.name()
                + "' is not supported. Use bare '" + n.name() + "' for the same library, "
                + "or fully-qualify like '@<group>/" + n.module() + "." + n.name() + "'."));
        }
        // Recurse into type arguments.
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

    private static final Set<String> BUILT_IN_PRIMITIVES = Set.of(
        "nil", "boolean", "number", "string", "thread", "buffer", "vector",
        "any", "unknown", "never");
}

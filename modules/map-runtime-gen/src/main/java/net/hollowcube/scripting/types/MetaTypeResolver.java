package net.hollowcube.scripting.types;

import com.palantir.javapoet.TypeName;
import net.hollowcube.scripting.Model;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// Rewrites a [Model.Library] so every `$`-prefixed [LuauType.Named] is substituted by its
/// [MetaType] expansion. After this pass runs there should be no meta-types left in the model;
/// downstream emitters (`SchemaJson`, `LibraryModuleEmitter`, `GlobalDeclEmitter`) operate on
/// the substituted form.
///
/// The rewrite is recursive and bottom-up — args to a meta-type are expanded first, so a
/// composition like `$Writable<$Writable<X>>` resolves inside-out without special handling.
/// Depth is capped to fail-fast on pathological inputs.
public final class MetaTypeResolver {

    /// Cap on recursive expansion depth. Real-world meta-type compositions are 1-2 deep; 32
    /// is comfortably past anything plausible while still cheap to detect.
    private static final int MAX_DEPTH = 32;

    private final SymbolTable symbols;
    private final List<ResolveDiagnostic> diagnostics;
    private final Map<String, Model.Export> exportsByJavaType;

    public MetaTypeResolver(SymbolTable symbols, Collection<Model.Library> allLibraries,
                            List<ResolveDiagnostic> diagnostics) {
        this.symbols = symbols;
        this.diagnostics = diagnostics;
        // Build a Java-type → Export map once at construction. SymbolTable holds the slim
        // SymbolEntry (strings only); we need the full Export to read its properties and walk
        // the superExport chain.
        this.exportsByJavaType = new HashMap<>();
        for (var lib : allLibraries)
            for (var ex : lib.exports())
                exportsByJavaType.put(ex.javaType().toString(), ex);
    }

    /// Return a new Library with every type expression rewritten. The original is unchanged.
    public Model.Library rewrite(Model.Library lib) {
        var staticMethods = rewriteMethods(lib, "static", lib.staticMethods());
        var staticProperties = rewriteProperties(lib, "static", lib.staticProperties());
        var exports = new ArrayList<Model.Export>(lib.exports().size());
        for (var ex : lib.exports()) exports.add(rewriteExport(lib, ex));
        return new Model.Library(
            lib.sourceType(), lib.glueType(), lib.moduleName(), lib.scope(),
            List.copyOf(exports), staticMethods, staticProperties, lib.description());
    }

    private Model.Export rewriteExport(Model.Library lib, Model.Export ex) {
        var props = rewriteProperties(lib, ex.luaName(), ex.properties());
        var methods = rewriteMethods(lib, ex.luaName(), ex.methods());
        var metas = rewriteMetaMethods(lib, ex.luaName(), ex.metaMethods());
        return new Model.Export(
            ex.javaType(), ex.luaName(), ex.superExport(), ex.isFinal(),
            ex.generics(), props, methods, metas,
            ex.userDataTag(), ex.hasSubtypes(),
            ex.kind(), ex.unionVariants(), ex.discriminator(), ex.description());
    }

    private List<Model.Property> rewriteProperties(Model.Library lib, String owner,
                                                   List<Model.Property> in) {
        var out = new ArrayList<Model.Property>(in.size());
        for (var p : in) {
            var getter = p.getter() == null ? null
                : rewriteAccessor(lib, owner + "." + p.luaName() + ":getter", p.getter());
            var setter = p.setter() == null ? null
                : rewriteAccessor(lib, owner + "." + p.luaName() + ":setter", p.setter());
            out.add(new Model.Property(p.luaName(), getter, setter));
        }
        return List.copyOf(out);
    }

    private Model.Accessor rewriteAccessor(Model.Library lib, String location, Model.Accessor a) {
        var newType = expand(a.type(), lib, location, 0);
        return new Model.Accessor(a.javaMethodName(), a.enclosingType(), a.description(),
            a.paramName(), newType);
    }

    private List<Model.Method> rewriteMethods(Model.Library lib, String owner,
                                              List<Model.Method> in) {
        var out = new ArrayList<Model.Method>(in.size());
        for (var m : in) {
            var loc = owner + "." + m.luaName();
            var params = rewriteParams(lib, loc, m.params());
            var returns = rewriteReturns(lib, loc, m.returns());
            out.add(new Model.Method(m.luaName(), m.javaMethodName(), m.isVoid(),
                m.enclosingType(), m.description(), m.generics(), params, returns));
        }
        return List.copyOf(out);
    }

    private List<Model.MetaMethod> rewriteMetaMethods(Model.Library lib, String owner,
                                                      List<Model.MetaMethod> in) {
        var out = new ArrayList<Model.MetaMethod>(in.size());
        for (var m : in) {
            var loc = owner + "." + m.meta();
            var params = rewriteParams(lib, loc, m.params());
            var returns = rewriteReturns(lib, loc, m.returns());
            out.add(new Model.MetaMethod(m.meta(), m.javaMethodName(), m.isVoid(),
                m.description(), m.generics(), params, returns));
        }
        return List.copyOf(out);
    }

    private List<Model.Param> rewriteParams(Model.Library lib, String owner, List<Model.Param> in) {
        var out = new ArrayList<Model.Param>(in.size());
        for (int i = 0; i < in.size(); i++) {
            var p = in.get(i);
            var loc = owner + ":param[" + i + "]:" + p.name();
            out.add(new Model.Param(p.name(), p.optional(), expand(p.type(), lib, loc, 0),
                p.description()));
        }
        return List.copyOf(out);
    }

    private List<Model.Return> rewriteReturns(Model.Library lib, String owner, List<Model.Return> in) {
        var out = new ArrayList<Model.Return>(in.size());
        for (int i = 0; i < in.size(); i++) {
            var r = in.get(i);
            var loc = owner + ":return[" + i + "]";
            out.add(new Model.Return(expand(r.type(), lib, loc, 0), r.description()));
        }
        return List.copyOf(out);
    }

    // =========================================================================
    // Core recursive expansion
    // =========================================================================

    /// Bottom-up rewrite of a single [LuauType]. Children are visited first; the result is
    /// then checked against the meta-type registry. Returns the expanded form (or the input
    /// unchanged when nothing applies).
    private LuauType expand(LuauType type, Model.Library lib, String location, int depth) {
        if (depth > MAX_DEPTH) {
            diagnostics.add(new ResolveDiagnostic(
                lib.moduleName() + ":" + location,
                "meta-type expansion exceeded depth " + MAX_DEPTH
                + " — likely a pathological composition"));
            return new LuauType.Named(null, "nil", List.of());
        }
        return switch (type) {
            case LuauType.Named n -> expandNamed(n, lib, location, depth);
            case LuauType.Optional o -> new LuauType.Optional(expand(o.inner(), lib, location, depth));
            case LuauType.Union u -> {
                var alts = new ArrayList<LuauType>(u.alternatives().size());
                for (var a : u.alternatives()) alts.add(expand(a, lib, location, depth));
                yield new LuauType.Union(List.copyOf(alts));
            }
            case LuauType.Intersection it -> {
                var conjs = new ArrayList<LuauType>(it.conjuncts().size());
                for (var c : it.conjuncts()) conjs.add(expand(c, lib, location, depth));
                yield new LuauType.Intersection(List.copyOf(conjs));
            }
            case LuauType.Table t -> {
                var fields = new ArrayList<LuauType.TableField>(t.fields().size());
                for (var f : t.fields())
                    fields.add(new LuauType.TableField(f.name(), expand(f.type(), lib, location, depth)));
                yield new LuauType.Table(
                    List.copyOf(fields),
                    t.arrayElement() == null ? null : expand(t.arrayElement(), lib, location, depth),
                    t.indexerKey() == null ? null : expand(t.indexerKey(), lib, location, depth),
                    t.indexerValue() == null ? null : expand(t.indexerValue(), lib, location, depth));
            }
            case LuauType.Function f -> {
                var params = new ArrayList<LuauType.Param>(f.params().size());
                for (var p : f.params())
                    params.add(new LuauType.Param(p.name(), expand(p.type(), lib, location, depth)));
                var varargs = f.varargs() == null ? null
                    : new LuauType.Variadic(expand(f.varargs().element(), lib, location, depth));
                var returns = new ArrayList<LuauType>(f.returns().size());
                for (var r : f.returns()) returns.add(expand(r, lib, location, depth));
                yield new LuauType.Function(List.copyOf(params), varargs, List.copyOf(returns));
            }
            case LuauType.Variadic v -> new LuauType.Variadic(expand(v.element(), lib, location, depth));
            // Leaves with no inner types — nothing to expand.
            case LuauType.GenericRef ignored -> type;
            case LuauType.GenericPack ignored -> type;
            case LuauType.TypeOf ignored -> type;
            case LuauType.StringLiteral ignored -> type;
            case LuauType.BoolLiteral ignored -> type;
        };
    }

    private LuauType expandNamed(LuauType.Named n, Model.Library lib, String location, int depth) {
        // Expand children (type args) bottom-up first.
        var newArgs = new ArrayList<LuauType.TypeArg>(n.args().size());
        for (var arg : n.args()) newArgs.add(expandTypeArg(arg, lib, location, depth + 1));
        var rebuilt = new LuauType.Named(n.module(), n.name(), List.copyOf(newArgs));

        // A `$`-prefixed bare name is a meta-type reference. Module-qualified meta-types
        // (`@mod.$Foo`) aren't supported — they'd never resolve anyway.
        if (n.module() != null || !MetaTypes.isMetaTypeName(n.name())) return rebuilt;

        var meta = MetaTypes.lookup(n.name());
        if (meta == null) {
            diagnostics.add(new ResolveDiagnostic(
                lib.moduleName() + ":" + location,
                "unknown meta-type '" + n.name() + "'"));
            return new LuauType.Named(null, "nil", List.of());
        }
        var ctx = new Ctx(lib, location);
        var expanded = meta.expand(rebuilt.args(), ctx);
        // Run the expansion through another pass so the meta-type can produce types containing
        // further meta-references (e.g. a future $Pick could emit a $Writable).
        return expand(expanded, lib, location, depth + 1);
    }

    private LuauType.TypeArg expandTypeArg(LuauType.TypeArg arg, Model.Library lib, String location, int depth) {
        return switch (arg) {
            case LuauType.TypeArg.Single s -> new LuauType.TypeArg.Single(expand(s.type(), lib, location, depth));
            case LuauType.TypeArg.Pack p -> {
                var types = new ArrayList<LuauType>(p.types().size());
                for (var t : p.types()) types.add(expand(t, lib, location, depth));
                var tail = p.tail() == null ? null
                    : new LuauType.Variadic(expand(p.tail().element(), lib, location, depth));
                yield new LuauType.TypeArg.Pack(List.copyOf(types), tail);
            }
        };
    }

    /// Per-use-site [MetaType.ExpansionContext] — carries the location string so meta-types can
    /// emit precise diagnostics without threading it themselves.
    private final class Ctx implements MetaType.ExpansionContext {
        private final Model.Library lib;
        private final String location;

        Ctx(Model.Library lib, String location) {
            this.lib = lib;
            this.location = location;
        }

        @Override
        public @Nullable Model.Export findExport(LuauType.Named ref) {
            String module = ref.module();
            SymbolEntry entry = module == null
                ? symbols.lookupBareInModule(lib.moduleName(), ref.name())
                : symbols.lookupByKey(module, ref.name());
            return entry == null ? null : findExportByJavaType(
                com.palantir.javapoet.ClassName.bestGuess(entry.javaType()));
        }

        @Override
        public @Nullable Model.Export findExportByJavaType(TypeName javaType) {
            return exportsByJavaType.get(javaType.toString());
        }

        @Override
        public Model.Library currentLibrary() {
            return lib;
        }

        @Override
        public void error(String location, String message) {
            diagnostics.add(new ResolveDiagnostic(lib.moduleName() + ":" + location, message));
        }

        @Override
        public String location() {
            return location;
        }
    }

}

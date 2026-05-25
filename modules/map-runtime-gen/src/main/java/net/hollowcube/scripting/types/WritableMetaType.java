package net.hollowcube.scripting.types;

import net.hollowcube.scripting.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/// `$Writable<T>` — resolves to a record table containing one optional field per writable
/// property of `T`. Concretely:
///
///  - For an `@LuaExport` argument: collect every [Model.Property] (walking the `superExport`
///    chain) whose `setter` is non-null. Each becomes a [LuauType.TableField] named after the
///    property's Lua name, typed as `Optional(setter.type())`.
///  - For an inline [LuauType.Table] argument: every field is treated as writable. Each field's
///    type is wrapped in [LuauType.Optional]. `arrayElement`/indexer are dropped with an error
///    (a "writable array" isn't meaningful).
///
/// Rejects:
///
///  - Wrong arity (`$Writable` with 0 or 2+ args).
///  - `UNION_ALIAS` parents (abstract; can't enumerate setters across variants safely).
///  - Unresolved type references.
///  - Non-table-like arguments (primitives, functions, generics, packs, literals, typeof).
///
/// On any rejection the expansion returns an empty table so the rest of the rewrite proceeds;
/// the diagnostic is what fails the build.
final class WritableMetaType implements MetaType {

    @Override
    public String name() {
        return "$Writable";
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public LuauType expand(List<LuauType.TypeArg> args, ExpansionContext ctx) {
        LuauType inner = singleScalarArg(args, ctx);
        if (inner == null) return emptyTable();

        return switch (inner) {
            case LuauType.Named n -> expandNamed(n, ctx);
            case LuauType.Table t -> expandInlineTable(t, ctx);
            default -> {
                ctx.error(ctx.location(),
                    "$Writable argument must be a known @LuaExport or an inline table type"
                    + " (got " + describe(inner) + ")");
                yield emptyTable();
            }
        };
    }

    /// Extract the single scalar arg from a [LuauType.TypeArg.Single] wrapper; report-and-bail
    /// for arity mismatch or a pack-shaped arg (`$Writable<(A, B)>` isn't a thing).
    private LuauType singleScalarArg(List<LuauType.TypeArg> args, ExpansionContext ctx) {
        if (args.size() != 1) {
            ctx.error(ctx.location(),
                "$Writable takes exactly 1 type argument, got " + args.size());
            return null;
        }
        return switch (args.getFirst()) {
            case LuauType.TypeArg.Single s -> s.type();
            case LuauType.TypeArg.Pack p -> {
                ctx.error(ctx.location(),
                    "$Writable does not accept a type pack argument");
                yield null;
            }
        };
    }

    /// Resolve a Named reference to its export, then collect writable properties up the
    /// superExport chain.
    private LuauType expandNamed(LuauType.Named ref, ExpansionContext ctx) {
        var export = ctx.findExport(ref);
        if (export == null) {
            ctx.error(ctx.location(),
                "$Writable argument '" + describe(ref) + "' is not a known @LuaExport");
            return emptyTable();
        }
        if (export.kind() == Model.Export.Kind.UNION_ALIAS) {
            ctx.error(ctx.location(),
                "$Writable<" + export.luaName() + "> is rejected: the type is a @LuaUnion alias."
                + " Use a concrete variant instead.");
            return emptyTable();
        }

        // Walk superExport chain. We need every ancestor's properties merged in; child wins on
        // name collision (child's setter overrides parent's, same as Java overrides).
        var byName = new LinkedHashMap<String, LuauType>();
        collectWritables(export, byName, ctx);
        var fields = new ArrayList<LuauType.TableField>(byName.size());
        for (var e : byName.entrySet()) fields.add(new LuauType.TableField(e.getKey(), e.getValue()));
        return new LuauType.Table(List.copyOf(fields), null, null, null);
    }

    /// Walk the superExport chain ancestor-first so child setters override ancestors on
    /// name collisions (mirrors Java method-override semantics).
    private void collectWritables(Model.Export start, LinkedHashMap<String, LuauType> byName,
                                  ExpansionContext ctx) {
        // Build the chain leaf→root, then iterate root→leaf so ancestor properties are
        // installed first and the leaf's `put` overrides them.
        var chain = new ArrayList<Model.Export>();
        var seen = new HashMap<String, Boolean>(); // Java type → visited
        var cur = start;
        while (cur != null) {
            if (seen.put(cur.javaType().toString(), true) != null) break; // cycle defense
            chain.add(cur);
            cur = cur.superExport() == null ? null : findExportByJavaType(cur.superExport(), ctx);
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            for (var p : chain.get(i).properties()) {
                if (p.setter() == null) continue;
                byName.put(p.luaName(), new LuauType.Optional(p.setter().type()));
            }
        }
    }

    /// Look up an export by its Java FQCN via the context. Returns null when the parent isn't
    /// in the registered symbol set — at that point the cross-module resolver has already
    /// flagged it, so we silently truncate the chain rather than double-reporting.
    private Model.Export findExportByJavaType(com.palantir.javapoet.TypeName javaType,
                                              ExpansionContext ctx) {
        return ctx.findExportByJavaType(javaType);
    }

    /// Inline-table case: rewrite every field as `name: Optional(type)`. Indexer/array are not
    /// expressible as "writable" so we drop them with an error.
    private LuauType expandInlineTable(LuauType.Table t, ExpansionContext ctx) {
        if (t.arrayElement() != null || t.indexerKey() != null) {
            ctx.error(ctx.location(),
                "$Writable on an inline table type cannot include an array element or indexer");
        }
        var fields = new ArrayList<LuauType.TableField>(t.fields().size());
        for (var f : t.fields())
            fields.add(new LuauType.TableField(f.name(), new LuauType.Optional(f.type())));
        return new LuauType.Table(List.copyOf(fields), null, null, null);
    }

    private static LuauType emptyTable() {
        return new LuauType.Table(List.of(), null, null, null);
    }

    /// Compact human-readable label for diagnostics — module-qualified when applicable.
    private static String describe(LuauType t) {
        if (t instanceof LuauType.Named n) {
            return (n.module() == null ? "" : n.module() + ".") + n.name();
        }
        return t.getClass().getSimpleName();
    }
}

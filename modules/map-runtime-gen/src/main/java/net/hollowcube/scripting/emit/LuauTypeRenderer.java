package net.hollowcube.scripting.emit;

import net.hollowcube.scripting.types.LuauType;

import java.util.List;
import java.util.StringJoiner;

/// Renders a [LuauType] AST back to Luau type-expression text — the exact inverse of
/// `LuauTypeParser`. `LuauTypeParser.parse(render(t, RenderContext.CANONICAL))` round-trips for
/// every variant except `TypeOf` (lexically lossy by design) and `GenericRef` (the parser only
/// ever produces `Named(null, name, [])`; the resolver reclassifies it later).
///
/// Parenthesization mirrors the parser's precedence: `Union`/`Intersection` are lowest, a
/// `Function`/`Union`/`Intersection` used as a union alternative, intersection conjunct, or
/// optional inner must be wrapped, and a non-trivial single function return is wrapped in a
/// return-list `( … )` so `(A) -> B | C` can't be misread.
public final class LuauTypeRenderer {

    private LuauTypeRenderer() {
    }

    public static String render(LuauType type, RenderContext ctx) {
        var sb = new StringBuilder();
        write(sb, type, ctx);
        return sb.toString();
    }

    private static void write(StringBuilder sb, LuauType type, RenderContext ctx) {
        switch (type) {
            case LuauType.Named n -> {
                sb.append(ctx.namedHead(n.module(), n.name()));
                if (!n.args().isEmpty()) {
                    sb.append('<');
                    var j = new StringJoiner(", ");
                    for (var a : n.args()) j.add(renderArg(a, ctx));
                    sb.append(j).append('>');
                }
            }
            case LuauType.GenericRef g -> sb.append(g.name());
            case LuauType.GenericPack gp -> sb.append(gp.name()).append("...");
            case LuauType.Optional o -> {
                sb.append(grouped(o.inner(), ctx));
                sb.append('?');
            }
            case LuauType.Union u -> {
                var j = new StringJoiner(" | ");
                for (var alt : u.alternatives()) j.add(grouped(alt, ctx));
                sb.append(j);
            }
            case LuauType.Intersection it -> {
                var j = new StringJoiner(" & ");
                for (var c : it.conjuncts()) j.add(grouped(c, ctx));
                sb.append(j);
            }
            case LuauType.Table t -> writeTable(sb, t, ctx);
            case LuauType.Function f -> writeFunction(sb, f, ctx);
            case LuauType.Variadic v -> {
                sb.append("...");
                write(sb, v.element(), ctx);
            }
            case LuauType.TypeOf to -> sb.append("typeof(").append(to.expr()).append(')');
            case LuauType.StringLiteral s -> sb.append('"').append(escape(s.value())).append('"');
            case LuauType.BoolLiteral b -> sb.append(b.value() ? "true" : "false");
        }
    }

    /// A type that needs wrapping when it appears as a union alternative, intersection conjunct,
    /// or optional inner — `Union`/`Intersection`/`Function` would otherwise re-associate.
    private static boolean needsGroup(LuauType t) {
        return t instanceof LuauType.Union
               || t instanceof LuauType.Intersection
               || t instanceof LuauType.Function;
    }

    private static String grouped(LuauType t, RenderContext ctx) {
        String r = render(t, ctx);
        return needsGroup(t) ? "(" + r + ")" : r;
    }

    private static void writeTable(StringBuilder sb, LuauType.Table t, RenderContext ctx) {
        if (t.arrayElement() != null) {
            sb.append('{');
            write(sb, t.arrayElement(), ctx);
            sb.append('}');
            return;
        }
        boolean hasIndexer = t.indexerKey() != null;
        if (t.fields().isEmpty() && !hasIndexer) {
            sb.append("{}");
            return;
        }
        sb.append("{ ");
        var j = new StringJoiner(", ");
        for (var f : t.fields()) j.add(f.name() + ": " + render(f.type(), ctx));
        if (hasIndexer) {
            j.add("[" + render(t.indexerKey(), ctx) + "]: " + render(t.indexerValue(), ctx));
        }
        sb.append(j).append(" }");
    }

    private static void writeFunction(StringBuilder sb, LuauType.Function f, RenderContext ctx) {
        sb.append('(');
        var j = new StringJoiner(", ");
        for (var p : f.params()) {
            j.add(p.name() == null ? render(p.type(), ctx) : p.name() + ": " + render(p.type(), ctx));
        }
        if (f.varargs() != null) j.add("..." + render(f.varargs().element(), ctx));
        sb.append(j).append(") -> ").append(renderReturns(f.returns(), ctx));
    }

    /// Render a return-type list as it appears after `->` (function types) or `:` (value
    /// function declarations): `()` for none, a bare or wrapped single, or `(A, B)` for many.
    public static String renderReturns(List<LuauType> rs, RenderContext ctx) {
        if (rs.isEmpty()) return "()";
        if (rs.size() == 1) {
            var only = rs.getFirst();
            String r = render(only, ctx);
            return bareReturn(only) ? r : "(" + r + ")";
        }
        var j = new StringJoiner(", ", "(", ")");
        for (var r : rs) j.add(render(r, ctx));
        return j.toString();
    }

    /// A single return type that does not start with `(` can be written bare after `->`. Types
    /// that render with a leading `(` (functions, grouped unions/intersections, parenthesized
    /// optionals) must be wrapped in a one-element return list so the parser doesn't read the
    /// `(` as the return list itself.
    private static boolean bareReturn(LuauType t) {
        return switch (t) {
            case LuauType.Union ignored -> false;
            case LuauType.Intersection ignored -> false;
            case LuauType.Function ignored -> false;
            case LuauType.GenericPack ignored -> false; // `-> (T...)`, never bare `-> T...`
            case LuauType.Optional o -> !needsGroup(o.inner());
            default -> true;
        };
    }

    private static String renderArg(LuauType.TypeArg arg, RenderContext ctx) {
        return switch (arg) {
            case LuauType.TypeArg.Single s -> render(s.type(), ctx);
            case LuauType.TypeArg.Pack p -> {
                if (p.types().isEmpty() && p.tail() != null) {
                    var elem = p.tail().element();
                    yield elem instanceof LuauType.GenericPack gp
                        ? gp.name() + "..."
                        : "..." + render(elem, ctx);
                }
                var j = new StringJoiner(", ", "(", ")");
                for (var t : p.types()) j.add(render(t, ctx));
                if (p.tail() != null) j.add("..." + render(p.tail().element(), ctx));
                yield j.toString();
            }
        };
    }

    private static String escape(String s) {
        var sb = new StringBuilder(s.length() + 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

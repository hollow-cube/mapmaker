package net.hollowcube.luau.engineapi.compat;

import net.hollowcube.luau.slopgen.types.LuauType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Strict structural equality on Luau types, modulo a small set of normalizations:
///
///  - Union member ordering (`string | number` ≡ `number | string`).
///  - `T | nil` ≡ `T?` (modeled as [LuauType.Optional]).
///  - Single-element parenthesization is already absorbed by the parser.
///
/// V1 has no notion of variance or `superExport`-aware subtyping — those graduations are
/// scheduled for a follow-up.
public final class LuauTypeRelation {

    private LuauTypeRelation() {
    }

    public static boolean equalsModuloNorm(LuauType a, LuauType b) {
        return normalize(a).equals(normalize(b));
    }

    public static LuauType normalize(LuauType ty) {
        return switch (ty) {
            case LuauType.Named n -> new LuauType.Named(n.module(), n.name(), normalizeArgs(n.args()));
            case LuauType.GenericRef g -> g;
            case LuauType.GenericPack gp -> gp;
            case LuauType.Optional o -> new LuauType.Optional(normalize(o.inner()));
            case LuauType.Union u -> normalizeUnion(u);
            case LuauType.Intersection i -> normalizeIntersection(i);
            case LuauType.Table t -> normalizeTable(t);
            case LuauType.Function f -> normalizeFunction(f);
            case LuauType.Variadic v -> new LuauType.Variadic(normalize(v.element()));
            case LuauType.TypeOf t -> t;
            case LuauType.StringLiteral s -> s;
            case LuauType.BoolLiteral b -> b;
        };
    }

    private static List<LuauType.TypeArg> normalizeArgs(List<LuauType.TypeArg> args) {
        var out = new ArrayList<LuauType.TypeArg>(args.size());
        for (var a : args) {
            switch (a) {
                case LuauType.TypeArg.Single s -> out.add(new LuauType.TypeArg.Single(normalize(s.type())));
                case LuauType.TypeArg.Pack p -> {
                    var ts = new ArrayList<LuauType>(p.types().size());
                    for (var t : p.types()) ts.add(normalize(t));
                    LuauType.Variadic tail = p.tail() == null ? null
                        : new LuauType.Variadic(normalize(p.tail().element()));
                    out.add(new LuauType.TypeArg.Pack(List.copyOf(ts), tail));
                }
            }
        }
        return List.copyOf(out);
    }

    private static LuauType normalizeUnion(LuauType.Union u) {
        var alts = new ArrayList<LuauType>();
        for (var a : u.alternatives()) alts.add(normalize(a));
        boolean hasNil = false;
        var nonNil = new ArrayList<LuauType>();
        for (var a : alts) {
            if (isNil(a)) hasNil = true;
            else nonNil.add(a);
        }
        nonNil.sort(Comparator.comparing(Object::toString));
        if (nonNil.size() == 1 && hasNil) return new LuauType.Optional(nonNil.get(0));
        if (nonNil.isEmpty() && hasNil) return new LuauType.Named(null, "nil", List.of());
        if (hasNil) {
            // Multi-alt + nil: keep as union with nil last.
            nonNil.add(new LuauType.Named(null, "nil", List.of()));
            return new LuauType.Union(List.copyOf(nonNil));
        }
        return new LuauType.Union(List.copyOf(nonNil));
    }

    private static LuauType normalizeIntersection(LuauType.Intersection i) {
        var conjs = new ArrayList<LuauType>();
        for (var c : i.conjuncts()) conjs.add(normalize(c));
        conjs.sort(Comparator.comparing(Object::toString));
        return new LuauType.Intersection(List.copyOf(conjs));
    }

    private static LuauType normalizeTable(LuauType.Table t) {
        var fields = new ArrayList<LuauType.TableField>();
        for (var f : t.fields()) fields.add(new LuauType.TableField(f.name(), normalize(f.type())));
        fields.sort(Comparator.comparing(LuauType.TableField::name));
        return new LuauType.Table(
            List.copyOf(fields),
            t.arrayElement() == null ? null : normalize(t.arrayElement()),
            t.indexerKey() == null ? null : normalize(t.indexerKey()),
            t.indexerValue() == null ? null : normalize(t.indexerValue()));
    }

    private static LuauType normalizeFunction(LuauType.Function f) {
        var params = new ArrayList<LuauType.Param>();
        for (var p : f.params()) params.add(new LuauType.Param(p.name(), normalize(p.type())));
        var returns = new ArrayList<LuauType>();
        for (var r : f.returns()) returns.add(normalize(r));
        return new LuauType.Function(
            List.copyOf(params),
            f.varargs() == null ? null : new LuauType.Variadic(normalize(f.varargs().element())),
            List.copyOf(returns));
    }

    private static boolean isNil(LuauType t) {
        return t instanceof LuauType.Named n
               && n.module() == null
               && n.name().equals("nil")
               && n.args().isEmpty();
    }
}

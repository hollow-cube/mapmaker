package net.hollowcube.luau.docs.types;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/// Abstract syntax tree for Luau type expressions, mirroring the `type` production in
/// <https://luau.org/grammar/>. The parser produces values of this hierarchy directly from raw
/// `@luaParam` / `@luaReturn` strings; the resolver later reclassifies [Named] nodes whose name
/// matches an in-scope `@luaGeneric` declaration into [GenericRef] form.
///
/// All variants are immutable records.
public sealed interface LuauType {

    /// Named or qualified-named type reference, e.g. `string`, `Player`, `players.Player`,
    /// `EventSource<(Player)>`. `module` is non-null only for the `module.Name` form.
    record Named(@Nullable String module, String name, List<TypeArg> args) implements LuauType {}

    /// Resolved generic-parameter reference. Produced by the resolve pass, never by the parser.
    record GenericRef(String name) implements LuauType {}

    /// `T?` syntactic sugar for `T | nil`.
    record Optional(LuauType inner) implements LuauType {}

    /// Two or more `T | U | …` alternatives.
    record Union(List<LuauType> alternatives) implements LuauType {}

    /// Two or more `T & U & …` conjuncts.
    record Intersection(List<LuauType> conjuncts) implements LuauType {}

    /// Either an array-style table `{T}`, a record-style `{ name: T, … }`, an indexer
    /// `{ [K]: V }`, or a mix. `arrayElement` is non-null when the table is a pure array; in
    /// that case both indexer fields are null and `fields` is empty. Otherwise either or both
    /// of `fields` and the indexer pair may be set.
    record Table(
        List<TableField> fields,
        @Nullable LuauType arrayElement,
        @Nullable LuauType indexerKey,
        @Nullable LuauType indexerValue
    ) implements LuauType {}

    record TableField(String name, LuauType type) {}

    /// Function type: `(P1, P2) -> R`, optionally with named params, varargs, and multi-return.
    record Function(
        List<Param> params,
        @Nullable Variadic varargs,
        List<LuauType> returns
    ) implements LuauType {}

    record Param(@Nullable String name, LuauType type) {}

    /// `...T` form, used in function param lists and as a pack tail in argument lists.
    record Variadic(LuauType element) implements LuauType {}

    /// `T...` form — a reference to a declared generic pack. The resolver enforces that `name`
    /// matches a declared `@luaGeneric T...` in scope.
    record GenericPack(String name) implements LuauType {}

    /// `typeof(expr)` form. The expression is preserved verbatim — Luau's expression grammar
    /// is large and this is rare in engine APIs. Kept as a string for v1.
    record TypeOf(String expr) implements LuauType {}

    /// String singleton type: `"create"`, `'destroy'`. The unquoted value is stored.
    record StringLiteral(String value) implements LuauType {}

    /// Boolean singleton type: `true` or `false`.
    record BoolLiteral(boolean value) implements LuauType {}

    /// One argument inside a generic instantiation `Foo<…>`. Either a single type or a pack.
    sealed interface TypeArg {
        /// A scalar type argument: the `T` in `Foo<T>`.
        record Single(LuauType type) implements TypeArg {}

        /// A pack of types: the `(T, U)` in `Foo<(T, U)>`. `tail` is non-null when the pack
        /// has a `...T` trailing element, e.g. `Foo<(A, ...B)>`.
        record Pack(List<LuauType> types, @Nullable Variadic tail) implements TypeArg {}
    }
}

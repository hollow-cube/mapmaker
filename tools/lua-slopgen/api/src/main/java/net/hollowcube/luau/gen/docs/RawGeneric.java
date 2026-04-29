package net.hollowcube.luau.gen.docs;

/// JSON representation of a `@luaGeneric` declaration. `pack` distinguishes the scalar form
/// `T` from the type-pack form `T...`.
public record RawGeneric(String name, boolean pack) {}

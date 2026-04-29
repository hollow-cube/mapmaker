package net.hollowcube.luau.slopgen.docs;

/// A single `@luaGeneric name[...]` declaration. `pack` is true when the source had `T...`
/// (a generic type pack), false for an ordinary scalar generic `T`.
public record TagGeneric(String name, boolean pack) {}

package net.hollowcube.luau.engineapi.emit;

import org.jetbrains.annotations.Nullable;

/// Strategy the renderer uses to turn a `Named`/`GenericRef` head into text. `module` is null
/// for a bare reference (builtin / same-file export / in-scope generic / ambient global) and an
/// `@`-module for a cross-library reference.
///
/// Implementations decide how a cross-library reference is written (a `require`d local binding
/// for module files, the canonical `@module.Name` for round-trip use) and may record the
/// dependency or reject it outright (an ambient global cannot `require`).
@FunctionalInterface
public interface RenderContext {

    /// Returns the rendered head (without any `<...>` argument list, which the renderer appends).
    String namedHead(@Nullable String module, String name);

    /// Canonical form: bare `name`, or `@module.Name` for a cross-library reference. Used for
    /// round-trip testing and any consumer that wants the resolver's qualified spelling back.
    RenderContext CANONICAL = (module, name) -> module == null ? name : module + "." + name;
}

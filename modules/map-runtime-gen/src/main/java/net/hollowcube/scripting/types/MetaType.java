package net.hollowcube.scripting.types;

import net.hollowcube.scripting.Model;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// A `$`-prefixed pseudo-type that exists only during codegen. The [MetaTypeResolver] walks
/// every [LuauType] tree before emit, and whenever it encounters a [LuauType.Named] whose name
/// matches a [MetaTypes#KNOWN] entry, it calls [#expand] to produce the real type that
/// downstream emitters see.
///
/// Implementations should be stateless; [ExpansionContext] carries everything they need.
public interface MetaType {

    /// The bare meta-type name including its `$` prefix, e.g. `$Writable`.
    String name();

    /// Number of type arguments. The resolver checks arity before calling [#expand].
    int arity();

    /// Produce the expansion. `args` are already meta-resolved bottom-up — the implementation
    /// never sees a `$`-prefixed Named inside them. Return any [LuauType]; emitters see this
    /// verbatim. On error, call [ExpansionContext#error] and return a placeholder (typically
    /// `Named(null, "nil", [])`) so the rewrite can continue.
    LuauType expand(List<LuauType.TypeArg> args, ExpansionContext ctx);

    /// Read-only view onto the surrounding analysis state, handed to [#expand] so the
    /// implementation doesn't need to capture mutable state itself.
    interface ExpansionContext {
        /// Look up the [Model.Export] that a `Named` refers to. Resolves both bare-in-module
        /// (`Foo`) and fully-qualified (`@mod.Foo`) forms. Returns null if the type isn't a
        /// known export.
        @Nullable Model.Export findExport(LuauType.Named ref);

        /// Look up an export by its Java FQCN. Used to walk a `superExport` chain since
        /// [Model.Export#superExport] is a `TypeName`, not a Luau name.
        @Nullable Model.Export findExportByJavaType(com.palantir.javapoet.TypeName javaType);

        /// The library that contains the original use site — needed for bare-name resolution.
        Model.Library currentLibrary();

        /// Record an error diagnostic. The resolver attributes it to the current library's
        /// originating element when reporting through `Messager`.
        void error(String location, String message);

        /// Human-readable location for the meta-type use site (e.g. `Player.teleport:param[0]`).
        /// Convenience for [#error] so implementations don't have to thread it manually.
        String location();
    }
}

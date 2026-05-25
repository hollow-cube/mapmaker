package net.hollowcube.scripting.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a Java `enum` for export as a Lua constant table + nominal type. Each enum constant
/// becomes a tagged light-userdata value addressable by its PascalCase Lua-side name
/// (`MAIN_HAND` → `MainHand`).
///
/// Two positions:
///
///  - **Top-level enum** (not nested in an `@LuaLibrary`): emitted as a global. The annotation's
///    `scope` must be [LuaLibrary.Scope#GLOBAL] (the default); declaring `scope = REQUIRE` on a
///    top-level enum is rejected.
///  - **Inner enum of an `@LuaLibrary`**: emitted as a field on that library's module value
///    (e.g. `local lib = require("@x/y"); lib.Slot.MainHand`). The annotation's `scope` field is
///    ignored — the library's own scope decides.
///
/// The annotated class must actually be a Java `enum` — annotating a regular class is a build
/// error.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LuaEnum {

    /// Optional override of the Lua-exported name. Defaults to the Java class's simple name.
    String name() default "";

    /// Position constraint for top-level enums. Inner enums ignore this field; their scope is
    /// the enclosing `@LuaLibrary`'s.
    LuaLibrary.Scope scope() default LuaLibrary.Scope.GLOBAL;
}

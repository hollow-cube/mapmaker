package net.hollowcube.luau.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// LuaLibrary configures the class to be registered as a require alias at the specified name
///
/// The name must start with @, eg `@test`, `@mapmaker/world`.
///
/// All static member [LuaMethod]s and [LuaProperty]s will be available on the module object.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LuaLibrary {
    String name();

    Scope scope() default Scope.REQUIRE;

    enum Scope {
        REQUIRE,
        GLOBAL,
    }
}

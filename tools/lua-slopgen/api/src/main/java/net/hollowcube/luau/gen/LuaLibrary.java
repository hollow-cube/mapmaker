package net.hollowcube.luau.gen;

import net.hollowcube.luau.annotation.old.LuaStatic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// LuaLibrary configures the class to be registered as a require alias at the specified name
///
/// The name must start with @, eg `@test`, `@mapmaker/world`.
///
/// All [LuaStatic]s will be available on the module object.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LuaLibrary {
    String name();
}

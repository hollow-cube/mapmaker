package net.hollowcube.luau.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// When annotating an inner class, indicates a type which will be exported from this
/// library. The inner class must be static.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LuaExport {

}

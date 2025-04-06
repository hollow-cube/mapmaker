package net.hollowcube.mapmaker.scripting.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is safe to call from an {@link org.graalvm.polyglot.HostAccess.Export}ed method.
 *
 * <p>This does not have any mechanical effect, just a hint to the caller that a method is safe.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ScriptSafe {
}

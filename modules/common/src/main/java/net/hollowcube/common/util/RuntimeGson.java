package net.hollowcube.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by native image helper to register relevant types for reflection by GSON.
 *
 * <p>Records are automatically registered, this is only necessary for non-records passed to GSON.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeGson {
}

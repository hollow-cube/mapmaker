package net.hollowcube.common.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by native image helper to register relevant types for reflection by GSON.
 *
 * <p>Every type gson reads or writes needs this, records included. The feature registers records
 * for reflection on its own, but only their constructors and component accessors, and gson builds
 * its bound fields from {@code getDeclaredFields()} even for a record. In a native image an
 * unregistered field set is an empty array rather than an error, so an unannotated record writes
 * as {@code {}} and reads back with every component null, silently.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RuntimeGson {
}

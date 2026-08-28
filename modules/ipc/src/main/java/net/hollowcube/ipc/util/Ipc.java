package net.hollowcube.ipc.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks the interface that defines one ipc service, from which both halves of it are generated.
///
/// A trailing `Service` is dropped from the name the generated classes are built on, so
/// `HeadDatabaseService` produces `HeadDatabaseClient` and `HeadDatabaseServer`.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Ipc {
}

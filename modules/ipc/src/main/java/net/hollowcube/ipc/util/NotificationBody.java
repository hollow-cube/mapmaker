package net.hollowcube.ipc.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a record as the `data` of every notification whose `type` column is [#type], which makes
/// it a wire root held to the compatibility rules.
///
/// Notification rows outlive both the process that wrote them and the one that reads them, so a
/// body is append-only: a new field has to be nullable, and nothing is ever renamed.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationBody {
    String type();
}

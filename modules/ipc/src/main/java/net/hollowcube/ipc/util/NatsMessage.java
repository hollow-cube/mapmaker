package net.hollowcube.ipc.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a record as the body of one NATS subject, which makes it a wire root: it and everything it
/// reaches is in `wire.json` and held to the compatibility rules, and its adapters are registered
/// on [net.hollowcube.ipc.Wire#gson].
///
/// The subject lives here rather than in whatever publishes or consumes the record so that the two
/// cannot spell it differently — a publisher on `request.rejected` and a consumer on
/// `invite.rejected` is the bug this exists to prevent.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NatsMessage {
    String subject();
}

package net.hollowcube.ipc.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a record as sent or stored outside an ipc call — a NATS message, a plugin message between
/// a game server and the proxy — under the key `value`: the subject or the channel. That makes it a
/// wire root: it and everything it reaches is in `wire.json` and held to the compatibility rules,
/// and its adapters are registered on [net.hollowcube.ipc.Wire#gson].
///
/// The key lives here rather than in whatever sends or receives the record so that the two cannot
/// spell it differently, and so that `wireCompat` sees a payload removed or rebound to another
/// record, which it would otherwise not: a type nothing reaches any more is simply off the wire.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Payload {
    String value();
}

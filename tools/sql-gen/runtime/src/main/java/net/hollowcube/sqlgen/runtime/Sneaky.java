package net.hollowcube.sqlgen.runtime;

/// Throws a [java.sql.SQLException] out of generated code without declaring it.
///
/// Generated query methods have no checked exceptions: a failed query is a bug or an outage, not
/// something a call site can meaningfully branch on. The cost is that `catch (SQLException e)`
/// around a call does not compile — catch [Exception] and test with `instanceof` instead, or handle
/// it around the whole transaction.
public final class Sneaky {

    /// Always throws `t`. The return type exists so call sites can write `throw Sneaky.rethrow(e)`
    /// and stay definitely-assigned.
    public static RuntimeException rethrow(Throwable t) {
        return sneak(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneak(Throwable t) throws E {
        throw (E) t;
    }

    private Sneaky() {
    }
}

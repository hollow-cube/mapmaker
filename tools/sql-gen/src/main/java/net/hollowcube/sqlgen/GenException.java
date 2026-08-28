package net.hollowcube.sqlgen;

/// A failure the developer has to fix in their SQL: a bad header, an unmappable type, a directive
/// that does not apply. The message is the whole error report, so it always names the query.
public final class GenException extends RuntimeException {

    public GenException(String message) {
        super(message);
    }

    public GenException(String message, Throwable cause) {
        super(message, cause);
    }
}

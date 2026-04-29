package net.hollowcube.luau.docs.types;

/// Thrown by [LuauTypeParser] when input does not match the grammar. Carries a `(offset, message)`
/// pair so callers can prepend the originating `file:line:column` context.
public final class LuauParseException extends RuntimeException {

    private final int offset;

    public LuauParseException(int offset, String message) {
        super("at offset " + offset + ": " + message);
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}

package net.hollowcube.common.parsing;

public final class ParsingException extends RuntimeException {

    private final int cursor;

    public ParsingException(int cursor, String message) {
        super(message);
        this.cursor = cursor;
    }

    public int cursor() {
        return cursor;
    }
}

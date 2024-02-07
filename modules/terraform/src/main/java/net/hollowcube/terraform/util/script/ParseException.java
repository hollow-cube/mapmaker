package net.hollowcube.terraform.util.script;

import org.jetbrains.annotations.NotNull;

public class ParseException extends Exception {

    private final int start, end;

    public ParseException(@NotNull String message) {
        this(-1, -1, message);
    }

    public ParseException(int start, int end, @NotNull String message) {
        super(message);
        this.start = start;
        this.end = end;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

}

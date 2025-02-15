package net.hollowcube.terraform.util.script;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Contract("null, _, _, _ -> fail")
    public static <T> T requireNonNull(@Nullable T obj, int start, int end, String message) throws ParseException {
        if (obj == null) {
            throw new ParseException(start, end, message);
        }
        return obj;
    }

}

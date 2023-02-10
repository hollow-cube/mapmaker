package net.hollowcube.terraform.mask.script;

import org.jetbrains.annotations.NotNull;

public class MaskParseException extends Exception {
    private final int start, end;

    public MaskParseException(@NotNull String message) {
        this(-1, -1, message);
    }

    public MaskParseException(int start, int end, @NotNull String message) {
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

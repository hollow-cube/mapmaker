package net.hollowcube.command.suggestion;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Suggestion {
    @ApiStatus.Internal
    public static final Suggestion EMPTY = new Suggestion(0, 0);

    private int start;
    private int length;
    private final List<SuggestionEntry> entries = new ArrayList<>();

    public Suggestion(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void add(@NotNull String suggestion) {
        add(suggestion, null);
    }

    public void add(@NotNull String suggestion, @Nullable Component tooltip) {
        entries.add(new SuggestionEntry(suggestion, tooltip));
    }

    public void clear() {
        entries.clear();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public @NotNull List<SuggestionEntry> getEntries() {
        return entries;
    }
}

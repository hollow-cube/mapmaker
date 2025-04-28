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

    private final int zero;
    private final List<SuggestionEntry> entries = new ArrayList<>();
    private int start;
    private int length;

    public Suggestion(int start, int length) {
        this.zero = start;
        this.start = start;
        this.length = length;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setAbsolute(int start) {
        this.start = this.zero + start;
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

    public void addAll(@NotNull Iterable<String> suggestions) {
        for (var suggestion : suggestions) {
            add(suggestion);
        }
    }

    public void addAll(@NotNull Suggestion other) {
        entries.addAll(other.entries);
    }
}

package net.hollowcube.mapmaker.storage;

import org.jetbrains.annotations.Nullable;

public interface Storage {
    RuntimeException NOT_FOUND = new RuntimeException("not found");
    RuntimeException DUPLICATE_ENTRY = new RuntimeException("already exists");

    static boolean isNotFound(@Nullable Throwable e) {
        if (e == null) return false;
        if (e == NOT_FOUND) return true;
        return isNotFound(e.getCause());
    }

    static boolean isDuplicateEntry(@Nullable Throwable e) {
        if (e == null) return false;
        if (e == DUPLICATE_ENTRY) return true;
        return isNotFound(e.getCause());
    }
}

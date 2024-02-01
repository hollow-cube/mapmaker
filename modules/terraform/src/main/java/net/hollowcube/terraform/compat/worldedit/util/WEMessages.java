package net.hollowcube.terraform.compat.worldedit.util;

import net.hollowcube.terraform.util.MessageSet;
import org.jetbrains.annotations.NotNull;

public enum WEMessages implements MessageSet {
    CLIPBOARD_PASTE_SELECT_ONLY("terraform.paste.select_only"),

    SELECTION_CHUNK("terraform.selchunk"),
    SELECTION_CHUNK_RANGE("terraform.selchunk.range"),
    SELECTION_EXPANDED("terraform.selection.expand"), // 0=added volume
    SELECTION_CONTRACTED("terraform.selection.contract"), // 0=removed volume
    SELECTION_OUTSET("terraform.selection.outset"), // 0=added volume
    SELECTION_INSET("terraform.selection.inset"), // 0=removed volume
    SELECTION_SHIFTED("terraform.selection.shift"),
    SELECTION_COUNT("terraform.selection.count"), // 0=count

    ;

    private final String key;

    WEMessages(@NotNull String key) {
        this.key = key;
    }

    @Override
    public @NotNull String key() {
        return key;
    }
}

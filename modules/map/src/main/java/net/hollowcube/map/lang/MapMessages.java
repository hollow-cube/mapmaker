package net.hollowcube.map.lang;

import net.hollowcube.common.lang.MessagesBase;
import org.jetbrains.annotations.NotNull;

public enum MapMessages implements MessagesBase {
    SCHEMATIC_UPLOAD_SUCCESS("map.schematic.upload.success"),
    ;

    private final String translationKey;

    MapMessages(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}

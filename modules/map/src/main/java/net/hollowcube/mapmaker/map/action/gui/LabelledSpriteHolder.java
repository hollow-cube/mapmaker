package net.hollowcube.mapmaker.map.action.gui;

import org.jetbrains.annotations.NotNull;

public interface LabelledSpriteHolder {

    @NotNull String label();

    @NotNull String sprite();

    default int spriteX() {
        return 0;
    }

    default int spriteY() {
        return 0;
    }
}

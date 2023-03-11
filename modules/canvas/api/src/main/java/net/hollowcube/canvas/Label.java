package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface Label extends Element {

    void setArgs(@NotNull Component... args);

}

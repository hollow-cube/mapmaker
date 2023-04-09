package net.hollowcube.canvas;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Label extends Element {

    void setArgs(@NotNull Component... args);

    default void setArgs(@NotNull List<Component> args) {
        setArgs(args.toArray(new Component[0]));
    }

}

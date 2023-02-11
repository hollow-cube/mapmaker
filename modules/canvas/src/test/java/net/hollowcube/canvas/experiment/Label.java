package net.hollowcube.canvas.experiment;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface Label {

    void setArgs(@NotNull Component... args);

    //todo move me to a separate interface (eg Element, but that is the impl)
    void setLoading(boolean loading);

}

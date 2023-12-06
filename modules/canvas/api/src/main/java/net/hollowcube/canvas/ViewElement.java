package net.hollowcube.canvas;

import org.jetbrains.annotations.NotNull;

public interface ViewElement extends Element {

    void addActionHandler(@NotNull String name, @NotNull Object handler, boolean async);

}

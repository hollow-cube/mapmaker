package net.hollowcube.canvas.view;

import org.jetbrains.annotations.NotNull;

public interface ParentView extends View {

    void add(int x, int y, @NotNull View view);

}

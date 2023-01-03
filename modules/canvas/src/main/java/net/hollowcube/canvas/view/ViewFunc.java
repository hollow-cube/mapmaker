package net.hollowcube.canvas.view;

import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ViewFunc {

    @Pure
    @NotNull View construct(@NotNull ViewContext context);

}

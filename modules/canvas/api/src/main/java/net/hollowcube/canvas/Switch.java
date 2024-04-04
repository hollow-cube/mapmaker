package net.hollowcube.canvas;

import org.jetbrains.annotations.Range;

public interface Switch extends Element {

    void setOption(@Range(from = 0, to = Integer.MAX_VALUE) int state);
    default void setOption(boolean state) {
        setOption(state ? 1 : 0);
    }

    int getOption();

}

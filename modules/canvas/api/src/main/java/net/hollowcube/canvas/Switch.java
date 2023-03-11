package net.hollowcube.canvas;

import org.jetbrains.annotations.Range;

public interface Switch {

    void setState(@Range(from = 0, to = Integer.MAX_VALUE) int state);

}

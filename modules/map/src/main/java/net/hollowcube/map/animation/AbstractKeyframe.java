package net.hollowcube.map.animation;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractKeyframe<Self> {
    private int time;

    public AbstractKeyframe(int time) {
        this.time = time;
    }

    public int time() {
        return time;
    }

    public abstract @NotNull Self copy();

    @Override
    public String toString() {
        return "Keyframe{" +
                "time=" + time +
                '}';
    }
}

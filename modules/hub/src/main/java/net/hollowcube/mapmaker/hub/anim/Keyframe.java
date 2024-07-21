package net.hollowcube.mapmaker.hub.anim;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record Keyframe(int t, @Nullable Runnable onStart, @NotNull Map<Channel<?>, Channel.Value> values) {
    public static final int NO_INTERP = 1 << 31;

    public Keyframe(int t) {
        this(t, null, new HashMap<>());
    }

    public Keyframe(int t, @NotNull Channel.Value... values) {
        this(t, null, format(values));
    }

    public Keyframe(int t, @NotNull Runnable onStart, @NotNull Channel.Value... values) {
        this(t, onStart, format(values));
    }

    @Override
    public int t() {
        if ((t & NO_INTERP) == NO_INTERP)
            return t & ~NO_INTERP;
        return t;
    }

    public boolean hasInterpolation() {
        return (t & NO_INTERP) != NO_INTERP;
    }

    private static @NotNull Map<Channel<?>, Channel.Value> format(@NotNull Channel.Value... values) {
        var m = new HashMap<Channel<?>, Channel.Value>();
        for (var value : values) m.put(value.channel(), value);
        return m;
    }

    public @NotNull Channel.Value getOrDefault(@NotNull Channel<?> channel) {
        return values.getOrDefault(channel, channel.defaultValue());
    }

    public @Nullable Channel.Value get(@NotNull Channel<?> channel) {
        return values.get(channel);
    }
}

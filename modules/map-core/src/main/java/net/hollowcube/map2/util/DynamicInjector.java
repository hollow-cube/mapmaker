package net.hollowcube.map2.util;

import com.google.inject.Injector;
import net.hollowcube.common.util.Injectors;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicInjector {
    private final Map<Class<?>, Object> bindings = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    private volatile Injector injector;

    public <T> void bind(@NotNull Class<T> clazz, @NotNull T instance) {
        bindings.put(clazz, Objects.requireNonNull(instance, clazz.getName()));
        dirty = true;
    }

    public @NotNull Injector injector() {
        if (dirty) {
            injector = Injectors.anonymous(bindings);
            dirty = false;
        }
        return injector;
    }
}

package net.hollowcube.common.util;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class Injectors {

    public static @NotNull Injector anonymous(@NotNull Map<Class<?>, Object> bindings) {
        return Guice.createInjector(anonymousModule(bindings));
    }

    public static @NotNull Injector child(@NotNull Injector parent, @NotNull Map<Class<?>, Object> bindings) {
        return parent.createChildInjector(anonymousModule(bindings));
    }

    public static @NotNull Module anonymousModule(@NotNull Map<Class<?>, Object> bindings) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                for (var entry : bindings.entrySet()) {
                    if (entry.getValue() == null)
                        throw new NullPointerException("Binding for " + entry.getKey() + " is null");
                    bind((Class<Object>) entry.getKey()).toInstance(entry.getValue());
                }
            }
        };
    }
}

package net.hollowcube.compat.axiom.properties.registry;

import net.hollowcube.compat.axiom.properties.PropertyCategory;
import net.hollowcube.compat.axiom.properties.PropertyDispatcher;
import net.hollowcube.compat.axiom.properties.WorldProperty;
import net.hollowcube.compat.axiom.properties.types.WidgetType;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class PropertyRegistry implements PropertyRegistrar {

    public static final PropertyRegistry INSTANCE = new PropertyRegistry();
    public static final Map<Key, WorldProperty<?>> PROPERTIES = new HashMap<>();
    public static final Map<PropertyCategory, List<WorldProperty<?>>> CATEGORIES = new ConcurrentHashMap<>();

    static {
        for (AxiomPropertyProvider provider : ServiceLoader.load(AxiomPropertyProvider.class)) {
            provider.registerProperties(INSTANCE);
        }
    }

    public static @Nullable WorldProperty<?> getProperty(Key id) {
        return PROPERTIES.get(id);
    }

    @Override
    public <T> WorldProperty<T> register(
        PropertyCategory category,
        Key id,
        String name, boolean localized,
        WidgetType<T> widget,
        T initialValue,
        PropertyDispatcher dispatcher
    ) {
        var property = new WorldProperty<>(id, name, localized, widget, initialValue, dispatcher);
        PROPERTIES.put(property.id(), property);
        CATEGORIES.computeIfAbsent(category, c -> new ArrayList<>()).add(property);
        return property;
    }
}

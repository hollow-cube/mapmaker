package net.hollowcube.compat.axiom.properties.registry;

import net.hollowcube.compat.axiom.properties.PropertyCategory;
import net.hollowcube.compat.axiom.properties.PropertyDispatcher;
import net.hollowcube.compat.axiom.properties.WorldProperty;
import net.hollowcube.compat.axiom.properties.types.WidgetType;
import net.kyori.adventure.key.Key;

public interface PropertyRegistrar {

    <T> WorldProperty<T> register(
        PropertyCategory category,
        Key id,
        String name, boolean localized,
        WidgetType<T> widget,
        T initialValue,
        PropertyDispatcher dispatcher
    );
}

package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.mapmaker.map.entity.MapEntity;

import java.util.ArrayList;
import java.util.List;

public class MapEntityInfo<E extends MapEntity<?>> {

    private final List<Property<?, ? super E>> properties;

    private MapEntityInfo(List<Property<?, ? super E>> entries) {
        this.properties = entries;
    }

    public List<Property<?, ? super E>> properties() {
        return this.properties;
    }

    // im sorry for how awful this is to use but java generics just suck at inference.
    public static <E extends MapEntity<?>> Builder<E> builder(MapEntityInfo<? super E> parent) {
        final var builder = new Builder<E>();
        parent.properties().forEach(builder::with);
        return builder;
    }

    public static <E extends MapEntity<?>> Builder<E> builder() {
        return new Builder<>();
    }

    public static class Builder<E extends MapEntity<?>> {

        private final List<Property<?, ? super E>> properties = new ArrayList<>();

        private Builder() {
        }

        public <T> Builder<E> with(String name, MapEntityInfoType<T, ? super E> type) {
            return with(new Property<>(name, type));
        }

        private <T> Builder<E> with(Property<T, ? super E> property) {
            properties.add(property);
            return this;
        }

        public MapEntityInfo<E> build() {
            return new MapEntityInfo<>(properties);
        }
    }

    public record Property<T, E extends MapEntity<?>>(String name, MapEntityInfoType<T, ? super E> type) {

    }
}

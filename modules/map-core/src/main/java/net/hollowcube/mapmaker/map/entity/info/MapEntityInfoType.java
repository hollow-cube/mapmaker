package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.component.DataComponent;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface MapEntityInfoType<T, E extends MapEntity<?>> {

    int DIALOG_OPTION_WIDTH = 250;

    T get(E entity);
    void set(E entity, T value);

    default @Nullable DialogInput toInput(E entity, String key, String label) {
        return null;
    }
    default void fromInput(E entity, BinaryTag data) {
        // no-op
    }

    @SuppressWarnings("unchecked")
    static @Nullable <T, E extends MapEntity<?>> DialogInput castToInput(MapEntity<?> entity, MapEntityInfoType<T, E> type, String key, String label) {
        return type.toInput((E) entity, key, label);
    }
    @SuppressWarnings("unchecked")
    static <T, E extends MapEntity<?>> void castFromInput(MapEntity<?> entity, MapEntityInfoType<T, E> type, BinaryTag data) {
        type.fromInput((E) entity, data);
    }

    static <T extends Enum<T>, M extends EntityMeta, E extends MapEntity<? extends M>> MapEntityInfoType<T, E> Enum(
        Class<T> type,
        T fallback,
        BiConsumer<M, T> setter,
        Function<M, T> getter
    ) {
        return new MapEntityInfoTypes.Enum<>(
            type,
            fallback,
            (entity, data) -> setter.accept(entity.getEntityMeta(), data),
            (entity) -> getter.apply(entity.getEntityMeta())
        );
    }

    static <M extends EntityMeta, E extends MapEntity<? extends M>> MapEntityInfoType<Boolean, E> Bool(
        boolean fallback,
        BiConsumer<M, Boolean> setter,
        Function<M, Boolean> getter
    ) {
        return new MapEntityInfoTypes.Bool<>(
            fallback,
            (entity, data) -> setter.accept(entity.getEntityMeta(), data),
            (entity) -> getter.apply(entity.getEntityMeta())
        );
    }

    static <E extends AbstractLivingEntity<?>> MapEntityInfoType<Double, E> Attribute(
        Attribute attribute
    ) {
        return Attribute(attribute, attribute.minValue(), attribute.maxValue());
    }

    static <E extends AbstractLivingEntity<?>> MapEntityInfoType<Double, E> Attribute(
        Attribute attribute,
        double min,
        double max
    ) {
        return new MapEntityInfoTypes.Float64<>(
            attribute.defaultValue(),
            min,
            max,
            0.1,
            (entity, data) -> entity.setAttribute(attribute, data),
            (entity) -> entity.getAttribute(attribute)
        );
    }

    static <M extends EntityMeta, E extends MapEntity<? extends M>> MapEntityInfoType<Double, E> Float64(
        double fallback,
        double min,
        double max,
        double step,
        BiConsumer<M, Double> setter,
        Function<M, Double> getter
    ) {
        return new MapEntityInfoTypes.Float64<>(
            fallback,
            min,
            max,
            step,
            (entity, data) -> setter.accept(entity.getEntityMeta(), data),
            (entity) -> getter.apply(entity.getEntityMeta())
        );
    }

    // Components

    static <T extends Enum<T>, E extends MapEntity<?>> MapEntityInfoType<T, E> Enum(
        Class<T> type,
        T fallback,
        DataComponent<T> component
    ) {
        return new MapEntityInfoTypes.Enum<>(
            type,
            fallback,
            (entity, data) -> entity.set(component, data),
            (meta) -> OpUtils.or(meta.get(component), () -> fallback)
        );
    }

    static <T extends Enum<T>, E extends MapEntity<?>> MapEntityInfoType<T, E> NullableEnum(
        Class<T> type,
        @Nullable T fallback,
        DataComponent<T> component
    ) {
        return new MapEntityInfoTypes.NullableEnum<>(
            type,
            fallback,
            (entity, data) -> entity.set(component, data),
            (meta) -> OpUtils.or(meta.get(component), () -> fallback)
        );
    }

    static <T, E extends MapEntity<?>> MapEntityInfoType<RegistryKey<T>, E> RegisteredKey(
        Function<Registries, Registry<T>> registry,
        RegistryKey<T> fallback,
        DataComponent<RegistryKey<T>> component
    ) {
        return new MapEntityInfoTypes.RegisteredKey<>(
            registry,
            fallback,
            (entity, data) -> entity.set(component, data),
            (meta) -> Objects.requireNonNullElse(meta.get(component), fallback)
        );
    }
}

package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoBaseTypes.EnumBase;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoBaseTypes.RegisteredKeyBase;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoBaseTypes.DeferredType;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MapEntityInfoTypes {

    public record Enum<T extends java.lang.Enum<T>, E extends MapEntity<?>>(
        Class<T> type,
        T fallback,
        BiConsumer<E, T> setter,
        Function<E, T> getter
    ) implements EnumBase<T, E> {

        public T[] values() {
            return type.getEnumConstants();
        }
    }

    public record Bool<E extends MapEntity<?>>(
        Boolean fallback,
        BiConsumer<E, Boolean> setter,
        Function<E, Boolean> getter
    ) implements DeferredType<Boolean, E> {

        @Override
        public DialogInput toInput(E entity, String key, String label) {
            var current = this.get(entity);
            return new DialogInput.SingleOption(
                key,
                DIALOG_OPTION_WIDTH,
                List.of(
                    new DialogInput.SingleOption.Option("true", Component.text("True"), current),
                    new DialogInput.SingleOption.Option("false", Component.text("False"), !current)
                ),
                Component.text(label),
                true
            );
        }

        @Override
        public void fromInput(E entity, BinaryTag data) {
            if (!(data instanceof StringBinaryTag tag)) return;
            this.set(entity, tag.value().equals("true"));
        }
    }

    public record Float64<E extends MapEntity<?>>(
        double fallback,
        double _min,
        double _max,
        double _step,
        BiConsumer<E, Double> setter,
        Function<E, Double> getter
    ) implements MapEntityInfoBaseTypes.NumberBase<Double, E> {

        @Override
        public Double min() {
            return _min;
        }

        @Override
        public Double max() {
            return _max;
        }

        @Override
        public Double step() {
            return _step;
        }

        @Override
        public Double parse(float value) {
            double steps = Math.round(value / _step);
            return Math.max(_min, Math.min(_max, steps * _step));
        }
    }

    public record NullableEnum<T extends java.lang.Enum<T>, E extends MapEntity<?>>(
        Class<T> type,
        @Nullable T fallback,
        BiConsumer<E, @Nullable T> setter,
        Function<E, @Nullable T> getter
    ) implements EnumBase<T, E> {

        @SuppressWarnings("unchecked")
        public @Nullable T[] values() {
            var constants = type.getEnumConstants();
            @Nullable var array = (T[]) Array.newInstance(type, constants.length + 1);
            array[0] = null;
            System.arraycopy(constants, 0, array, 1, constants.length);
            return array;
        }
    }

    public record RegisteredKey<T, E extends MapEntity<?>>(
        Function<Registries, Registry<T>> registry,
        RegistryKey<T> fallback,
        BiConsumer<E, RegistryKey<T>> setter,
        Function<E, RegistryKey<T>> getter
    ) implements RegisteredKeyBase<T, E> {

    }
}

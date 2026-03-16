package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.NumberBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.dialog.DialogInput;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapEntityInfoBaseTypes {

    interface DeferredType<T, E extends MapEntity<?>> extends MapEntityInfoType<T, E> {
        BiConsumer<E, T> setter();
        Function<E, T> getter();

        @Override
        default T get(E entity) {
            return this.getter().apply(entity);
        }

        @Override
        default void set(E entity, T value) {
            this.setter().accept(entity, value);
        }
    }

    interface EnumBase<T extends java.lang.Enum<T>, E extends MapEntity<?>> extends DeferredType<T, E> {
        Class<T> type();
        @Nullable T[] values();

        @Override
        default DialogInput toInput(E entity, String key, String label) {
            var current = this.get(entity);
            return new DialogInput.SingleOption(
                key,
                DIALOG_OPTION_WIDTH,
                Arrays.stream(values())
                    .map(it -> new DialogInput.SingleOption.Option(
                        it == null ? "" : it.name(),
                        Component.text(it == null ? "None" : capitalize(it)),
                        it == current
                    ))
                    .toList(),
                Component.text(label),
                true
            );
        }

        @Override
        default void fromInput(E entity, BinaryTag data) {
            if (!(data instanceof StringBinaryTag tag)) return;
            try {
                this.set(entity, tag.value().isEmpty() ? null : Enum.valueOf(this.type(), tag.value()));
            } catch (IllegalArgumentException _) {
                // ignore invalid values
            }
        }
    }

    interface RegisteredKeyBase<T, E extends MapEntity<?>> extends MapEntityInfoType<RegistryKey<T>, E> {
        Function<Registries, Registry<T>> registry();
        BiConsumer<E, RegistryKey<T>> setter();
        Function<E, RegistryKey<T>> getter();

        @Override
        default RegistryKey<T> get(E entity) {
            return this.getter().apply(entity);
        }

        @Override
        default void set(E entity, RegistryKey<T> value) {
            this.setter().accept(entity, value);
        }

        @Override
        default DialogInput toInput(E entity, String key, String label) {
            var current = this.get(entity);
            return new DialogInput.SingleOption(
                key,
                DIALOG_OPTION_WIDTH,
                this.registry().apply(MinecraftServer.process())
                    .keys()
                    .stream()
                    .map(it -> new DialogInput.SingleOption.Option(
                        it.name(),
                        Component.text(it.name()),
                        it.equals(current)
                    ))
                    .toList(),
                Component.text(label),
                true
            );
        }

        @Override
        default void fromInput(E entity, BinaryTag data) {
            if (!(data instanceof StringBinaryTag tag)) return;
            try {
                var registry = this.registry().apply(MinecraftServer.process());
                var key = registry.getKey(Key.key(tag.value()));
                if (key == null) return;
                this.set(entity, key);
            } catch (InvalidKeyException _) {
                // ignore invalid values
            }
        }
    }

    interface NumberBase<T extends Number, E extends MapEntity<?>> extends DeferredType<T, E> {
        T min();
        T max();
        T step();
        T parse(float value);

        @Override
        default DialogInput toInput(E entity, String key, String label) {
            var current = this.get(entity);

            return new DialogInput.NumberRange(
                key,
                DIALOG_OPTION_WIDTH,
                Component.text(label),
                "options.generic_value",
                this.min().floatValue(),
                this.max().floatValue(),
                current.floatValue(),
                this.step().floatValue()
            );
        }

        @Override
        default void fromInput(E entity, BinaryTag data) {
            float value;
            if (data instanceof NumberBinaryTag numberTag) {
                value = numberTag.floatValue();
            } else if (data instanceof StringBinaryTag stringTag) {
                try {
                    value = Float.parseFloat(stringTag.value());
                } catch (NumberFormatException _) {
                    return; // ignore invalid values
                }
            } else {
                return;
            }

            if (value >= this.min().floatValue() && value <= this.max().floatValue()) {
                this.set(entity, this.parse(value));
            }
        }
    }

    private static String capitalize(Enum<?> value) {
        return Arrays.stream(value.name().toLowerCase().split("_"))
            .map(it -> {
                if (it.isEmpty()) return it;
                return Character.toUpperCase(it.charAt(0)) + it.substring(1);
            })
            .collect(Collectors.joining(" "));
    }
}

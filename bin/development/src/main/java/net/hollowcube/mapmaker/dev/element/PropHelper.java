package net.hollowcube.mapmaker.dev.element;

import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class PropHelper {

    static @NotNull String getString(Value props, String key) {
        return Objects.requireNonNull(getString(props, key, null), () -> key + " is required");
    }

    static @UnknownNullability String getString(Value props, String key, String defaultValue) {
        if (props.hasMember(key)) {
            return props.getMember(key).asString();
        }
        return defaultValue;
    }

    static @UnknownNullability Integer getInt(Value props, String key, Integer defaultValue) {
        if (props.hasMember(key)) {
            return props.getMember(key).asInt();
        }
        return defaultValue;
    }

    static @NotNull List<Node> childrenAsList(@NotNull Value[] children) {
        System.out.println(Arrays.toString(children));
        return Arrays.stream(children)
                .filter(Value::isHostObject)
                .map(Value::asHostObject)
                .map(Node.class::cast)
                .toList();
    }

    static <E extends Enum<E>> @NotNull E getEnum(Value props, String key, @NotNull E defaultValue) {
        if (props.hasMember(key)) {
            return Enum.valueOf(defaultValue.getDeclaringClass(),
                    getString(props, key).toUpperCase(Locale.ROOT));
        }
        return defaultValue;
    }
}

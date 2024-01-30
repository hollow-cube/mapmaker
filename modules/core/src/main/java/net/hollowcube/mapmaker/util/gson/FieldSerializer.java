package net.hollowcube.mapmaker.util.gson;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.function.Function;

public class FieldSerializer<T, F> implements JsonSerializer<T>, JsonDeserializer<T> {
    private final Function<F, T> constructor;
    private final Function<T, F> getter;
    private final Type fieldType;

    public FieldSerializer(@NotNull Function<F, T> constructor, @NotNull Function<T, F> getter, @NotNull Type fieldType) {
        this.constructor = constructor;
        this.getter = getter;
        this.fieldType = fieldType;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return constructor.apply(context.deserialize(json, fieldType));
    }

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(getter.apply(src));
    }
}

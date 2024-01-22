package net.hollowcube.mapmaker.util.gson;

import com.google.gson.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class OptionalTypeAdapter implements JsonDeserializer<Optional<?>>, JsonSerializer<Optional<?>> {

    @Override
    public Optional<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final Object value = context.deserialize(json, ((ParameterizedType) typeOfT).getActualTypeArguments()[0]);
        return Optional.ofNullable(value);
    }

    @Override
    public JsonElement serialize(Optional<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.orElse(null));
    }

}

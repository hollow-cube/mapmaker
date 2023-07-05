package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class EnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Class<T> cls;

    public EnumTypeAdapter(@NotNull Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.name().toLowerCase());
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        return Enum.valueOf(cls, in.nextString().toUpperCase());
    }
}

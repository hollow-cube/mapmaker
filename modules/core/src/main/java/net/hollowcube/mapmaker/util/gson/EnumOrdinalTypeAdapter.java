package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class EnumOrdinalTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Class<T> cls;

    private final T[] values;

    public EnumOrdinalTypeAdapter(@NotNull Class<T> cls) {
        this.cls = cls;
        this.values = cls.getEnumConstants();
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.ordinal());
    }

    @Override
    public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        int i = in.nextInt();
        if (i < 0 || i >= values.length) {
            throw new IOException("Invalid ordinal for " + cls.getSimpleName() + ": " + i);
        }
        return values[i];
    }
}

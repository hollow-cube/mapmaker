package net.hollowcube.mapmaker.util.gson;

import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;

public class UnsignedLongAdapter extends TypeAdapter<Long> {

    public UnsignedLongAdapter() {
    }

    @Override
    public void write(JsonWriter out, Long value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(Long.toUnsignedString(value));
        }
    }

    @Override
    public Long read(JsonReader in) throws IOException {
        final var input = in.nextString();
        return input.isBlank() ? 0 : Long.parseUnsignedLong(input);
    }

    public static class Creator implements InstanceCreator<UnsignedLongAdapter> {

        @Override
        public UnsignedLongAdapter createInstance(Type type) {
            return new UnsignedLongAdapter();
        }
    }
}

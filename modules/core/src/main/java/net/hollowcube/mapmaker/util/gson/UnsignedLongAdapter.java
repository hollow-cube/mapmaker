package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

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
        return Long.parseUnsignedLong(in.nextString());
    }
}

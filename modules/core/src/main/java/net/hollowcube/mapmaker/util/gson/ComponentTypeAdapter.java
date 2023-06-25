package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;

public class ComponentTypeAdapter extends TypeAdapter<Component> {
    @Override
    public void write(JsonWriter out, Component value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.jsonValue(GsonComponentSerializer.gson().serialize(value));
    }

    @Override
    public Component read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        return GsonComponentSerializer.gson().deserialize(in.nextString());
    }
}

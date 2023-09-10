package net.hollowcube.mapmaker.util.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.hollowcube.mapmaker.object.ObjectType;

import java.io.IOException;

public class ObjectTypeTypeAdapter extends TypeAdapter<ObjectType> {
    @Override
    public void write(JsonWriter out, ObjectType value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.jsonValue(value.id());
    }

    @Override
    public ObjectType read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        return ObjectType.find(in.nextString());
    }
}

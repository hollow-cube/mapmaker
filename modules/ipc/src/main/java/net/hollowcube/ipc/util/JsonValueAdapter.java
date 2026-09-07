package net.hollowcube.ipc.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/// Preserves explicit nulls inside opaque JSON while optional record fields still omit nulls.
public final class JsonValueAdapter extends TypeAdapter<JsonElement> {
    private static final TypeAdapter<JsonElement> DELEGATE = new Gson()
        .getAdapter(JsonElement.class);

    @Override
    public void write(JsonWriter out, @Nullable JsonElement value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        var serializeNulls = out.getSerializeNulls();
        out.setSerializeNulls(true);
        try {
            DELEGATE.write(out, value);
        } finally {
            out.setSerializeNulls(serializeNulls);
        }
    }

    @Override
    public JsonElement read(JsonReader in) throws IOException {
        return DELEGATE.read(in);
    }
}

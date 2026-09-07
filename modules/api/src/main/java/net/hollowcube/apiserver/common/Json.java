package net.hollowcube.apiserver.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

public final class Json {

    /// A nullable jsonb column as an object; sql-gen hands jsonb over as its text. The JSON literal
    /// `null` counts as empty too: pgx marshals Go's nil map that way, so rows Go wrote hold it.
    public static JsonObject object(@Nullable String column) {
        if (column == null) return new JsonObject();
        var json = JsonParser.parseString(column);
        return json.isJsonNull() ? new JsonObject() : json.getAsJsonObject();
    }

    private Json() {}
}

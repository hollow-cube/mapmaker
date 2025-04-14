package net.hollowcube.datafix.versions.v0xxx;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V165 extends DataVersion {
    public V165() {
        super(165);

        addFix(DataTypes.TEXT_COMPONENT, V165::fixTextComponentStrictJson);
    }

    private static Value fixTextComponentStrictJson(Value value) {
        if (!(value.value() instanceof String s)) return value;
        if (s.isEmpty() || "null".equals(s))
            return Value.wrap("{\"text\":\"\"}");

        char c = s.charAt(0);
        char d = s.charAt(s.length() - 1);
        if (c == '"' && d == '"' || c == '{' && d == '}' || c == '[' && d == ']') {
            try {
                JsonElement jsonElement = JsonParser.parseString(s);
                if (jsonElement.isJsonPrimitive()) {
                    return Value.wrap("{\"text\":\"" + jsonElement.getAsString() + "\"}");
                }

                // Used to use toStableString, shouldnt matter here?
                return Value.wrap(jsonElement.toString());
            } catch (JsonParseException ignored) {
            }
        }

        return Value.wrap("{\"text\":\"" + s + "\"}");


    }
}

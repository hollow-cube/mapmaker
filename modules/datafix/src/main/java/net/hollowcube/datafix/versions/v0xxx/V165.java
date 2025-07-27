package net.hollowcube.datafix.versions.v0xxx;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.List;

public class V165 extends DataVersion {
    private static final List<String> LINE_FIELDS = List.of("Text1", "Text2", "Text3", "Text4");

    public V165() {
        super(165);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:sign", V165::fixSignTextComponentStrictJson);
        addFix(DataTypes.ITEM_STACK, "minecraft:written_book", V165::fixWrittenBookTextComponentStrictJson);
    }

    private static Value fixSignTextComponentStrictJson(Value value) {
        for (var field : LINE_FIELDS)
            value.put(field, fixTextComponentStrictJson(value.get(field)));
        return null;
    }

    private static Value fixWrittenBookTextComponentStrictJson(Value value) {
        var pages = value.get("tag").get("pages");
        for (int i = 0; i < pages.size(0); i++) {
            value.put(i, fixTextComponentStrictJson(value.get(i)));
        }
        return null;
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

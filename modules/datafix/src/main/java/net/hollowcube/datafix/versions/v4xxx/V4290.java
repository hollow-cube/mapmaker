package net.hollowcube.datafix.versions.v4xxx;

import com.google.gson.*;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.DataFixUtils;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V4290 extends DataVersion {

    public V4290() {
        super(4290);

        addReference(DataTypes.TEXT_COMPONENT, field -> field
                // Can be a self referential list
                .list("", DataTypes.TEXT_COMPONENT)
                // Below is the object form
                .list("extra", DataTypes.TEXT_COMPONENT)
                .single("separator", DataTypes.TEXT_COMPONENT)
                .single("hoverEvent.contents", DataTypes.TEXT_COMPONENT)
                .single("hoverEvent.contents", DataTypes.ITEM_STACK)
                .single("hoverEvent.contents", DataTypes.ITEM_NAME)
                .single("hoverEvent.type", DataTypes.ENTITY_NAME)
                .single("hoverEvent.name", DataTypes.TEXT_COMPONENT));

        addFix(DataTypes.TEXT_COMPONENT, V4290::fixParseJsonComponents);
    }

    private static Value fixParseJsonComponents(Value value) {
        if (value.value() instanceof String raw) {
            try {
                return fixTextComponent(raw, DataFixUtils.GSON.fromJson(raw, JsonElement.class));
            } catch (JsonSyntaxException | IllegalStateException | IllegalArgumentException ignored) {
                var text = Value.emptyMap();
                text.put("text", raw);
                return text;
            }
        } else if (value.isMapLike()) {
            var wrappedValue = value.remove("");
            if (!wrappedValue.isNull()) {
                value.put("text", wrappedValue);
            }
        }
        return null;
    }

    private static Value fixTextComponent(String raw, JsonElement elem) {
        return switch (elem) {
            case JsonObject obj -> Value.wrap(DataFixUtils.GSON.fromJson(obj, Map.class));
            case JsonArray arr -> {
                var list = Value.emptyList();
                for (JsonElement child : arr) {
                    list.put(fixTextComponent(child.toString(), child));
                }
                yield list;
            }
            case JsonPrimitive prim -> Value.wrap(prim.getAsString());
            case null, default -> {
                var text = Value.emptyMap();
                text.put("text", raw);
                yield text;
            }
        };
    }

}

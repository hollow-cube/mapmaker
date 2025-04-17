package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import net.minestom.server.codec.Transcoder;

public class V4291 extends DataVersion {
    public V4291() {
        super(4291);

        addFix(DataTypes.TEXT_COMPONENT, V4291::fixTextComponentHoverEventInnerComponent);
        addFix(DataTypes.TEXT_COMPONENT, V4291::fixComponentStringifiedFlags);
    }

    private static Value fixTextComponentHoverEventInnerComponent(Value textComponent) {
        if (!textComponent.isMapLike()) return null; // list or single string
        var hoverEvent = textComponent.get("hoverEvent");
        if (!hoverEvent.isMapLike()) return null;

        var action = hoverEvent.get("action").as(String.class, "");
        if ("show_text".equals(action)) {
            hoverEvent.put("contents", hoverEvent.remove("action"));
        } else {
            var placeholder = Value.emptyMap();
            placeholder.put("action", "show_text");
            var json = Value.TRANSCODER.convertTo(Transcoder.JSON, hoverEvent).orElseThrow();
            placeholder.put("contents", "Legacy hoverEvent: " + json);
            textComponent.put("hoverEvent", placeholder);
        }

        return null;
    }

    private static Value fixComponentStringifiedFlags(Value textComponent) {
        textComponent.put("bold", stringToBool(textComponent.get("bold")));
        textComponent.put("italic", stringToBool(textComponent.get("italic")));
        textComponent.put("underlined", stringToBool(textComponent.get("underlined")));
        textComponent.put("strikethrough", stringToBool(textComponent.get("strikethrough")));
        textComponent.put("obfuscated", stringToBool(textComponent.get("obfuscated")));
        return null;
    }

    private static Value stringToBool(Value value) {
        var string = value.as(String.class, null);
        if (string == null) return value;
        return Value.wrap(Boolean.parseBoolean(string));
    }

}

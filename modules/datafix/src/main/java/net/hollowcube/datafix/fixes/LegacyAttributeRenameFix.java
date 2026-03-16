package net.hollowcube.datafix.fixes;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record LegacyAttributeRenameFix(Map<String, String> renames) {

    public @Nullable Value fixInItemStack(Value itemStack) {
        var tag = itemStack.get("tag");
        if (!tag.isMapLike()) return null;

        for (var attribute : tag.get("AttributeModifiers")) {
            var name = attribute.get("AttributeName").as(String.class, null);
            if (name != null) attribute.put("AttributeName", renames.getOrDefault(name, name));
        }

        return null;
    }

    public @Nullable Value fixInEntity(Value entity) {
        for (var attribute : entity.get("Attributes")) {
            var name = attribute.get("Name").as(String.class, null);
            if (name != null) attribute.put("Name", renames.getOrDefault(name, name));
        }
        return null;
    }

}

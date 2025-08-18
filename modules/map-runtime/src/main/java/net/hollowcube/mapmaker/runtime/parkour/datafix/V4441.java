package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

@AutoService(ExternalDataFix.class)
public class V4441 extends DataVersion implements ExternalDataFix {

    public V4441() {
        super(4441);

        addFix(DataTypes.ITEM_STACK, V4441::fixBrokenAttributeModifiers);
    }

    private static Value fixBrokenAttributeModifiers(@NotNull Value data) {
        var components = data.get("components");
        var attributeModifiers = components.get("minecraft:attribute_modifiers");
        if (attributeModifiers.isMapLike() && attributeModifiers.size(0) == 0)
            // Remove empty attribute modifiers
            components.remove("minecraft:attribute_modifiers");
        return null;
    }
}

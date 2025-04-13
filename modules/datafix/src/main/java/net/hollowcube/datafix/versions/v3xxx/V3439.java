package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V3439 extends DataVersion {
    public V3439() {
        super(3439);

        addReference(DataType.BLOCK_ENTITY, "minecraft:sign", V3439::signBlock);
        addReference(DataType.BLOCK_ENTITY, "minecraft:hanging_sign", V3439::signBlock);
    }

    static @NotNull Field signBlock(@NotNull Field field) {
        return field.list("front_text.messages", DataType.TEXT_COMPONENT)
                .list("front_text.filtered_messages", DataType.TEXT_COMPONENT)
                .list("back_text.messages", DataType.TEXT_COMPONENT)
                .list("back_text.filtered_messages", DataType.TEXT_COMPONENT);
    }
}

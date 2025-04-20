package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class V3439 extends DataVersion {
    static final List<String> FIELDS_TO_DROP = List.of(
            "Text1", "Text2", "Text3", "Text4",
            "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4",
            "Color", "GlowingText"
    );
    public static final String EMPTY_TEXT = "{\"text\":\"\"}";

    public V3439() {
        super(3439);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:sign", V3439::signBlock);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:hanging_sign", V3439::signBlock);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:sign", V3439::fixSignBlockEntityDoubleSidedText);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:hanging_sign", V3439::fixSignBlockEntityDoubleSidedText);
    }

    public static @NotNull DataType.Builder signBlock(@NotNull DataType.Builder field) {
        return field.list("front_text.messages", DataTypes.TEXT_COMPONENT)
                .list("front_text.filtered_messages", DataTypes.TEXT_COMPONENT)
                .list("back_text.messages", DataTypes.TEXT_COMPONENT)
                .list("back_text.filtered_messages", DataTypes.TEXT_COMPONENT);
    }

    private static Value fixSignBlockEntityDoubleSidedText(Value blockEntity) {
        blockEntity.put("front_text", fixFrontText(blockEntity));
        blockEntity.put("back_text", createBackText());
        blockEntity.put("is_waxed", false);
        blockEntity.put("_filtered_correct", true);

        FIELDS_TO_DROP.forEach(blockEntity::remove);
        return null;
    }

    private static Value fixFrontText(Value blockEntity) {
        var frontText = Value.emptyMap();

        var messages = Value.emptyList();
        for (int i = 0; i < 4; i++)
            messages.put(blockEntity.get("Text" + (i + 1)).as(String.class, EMPTY_TEXT));
        frontText.put("messages", messages);

        frontText.put("color", blockEntity.get("Color").as(String.class, "black"));
        frontText.put("has_glowing_text", blockEntity.get("GlowingText").as(Boolean.class, false));

        boolean hasFilteredText = false;
        var filteredMessages = Value.emptyList();
        for (int i = 0; i < 4; i++) {
            var filteredLine = blockEntity.get("FilteredText" + (i + 1));
            if (!filteredLine.isNull()) {
                filteredMessages.put(filteredLine.as(String.class, EMPTY_TEXT));
                hasFilteredText = true;
            } else {
                filteredMessages.put(messages.get("Text" + (i + 1)).as(String.class, EMPTY_TEXT));
            }
        }
        if (hasFilteredText) {
            frontText.put("filtered_messages", filteredMessages);
        }

        return frontText;
    }

    private static Value createBackText() {
        var backText = Value.emptyMap();
        var backMessages = Value.emptyList();
        for (int i = 0; i < 4; i++) backMessages.put(EMPTY_TEXT);
        backText.put("messages", backMessages);
        backText.put("color", "black");
        backText.put("has_glowing_text", false);
        return backText;
    }
}

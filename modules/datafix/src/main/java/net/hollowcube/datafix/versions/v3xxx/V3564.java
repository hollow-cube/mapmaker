package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3564 extends DataVersion {
    public V3564() {
        super(3564);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:sign", V3564::fixDropInvalidSignData);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:hanging_sign", V3564::fixDropInvalidSignData);
    }

    private static Value fixDropInvalidSignData(Value blockEntity) {
        if (blockEntity.remove("_filtered_correct").as(Boolean.class, false))
            return blockEntity;

        fixFrontBackData(blockEntity.get("front_text"));
        fixFrontBackData(blockEntity.get("back_text"));

        V3439.FIELDS_TO_DROP.forEach(blockEntity::remove);
        return null;
    }

    private static void fixFrontBackData(Value data) {
        var filteredMessages = data.get("filtered_messages");
        var filteredMessagesCount = filteredMessages.size(0);
        if (filteredMessagesCount == 0) return;

        var messages = data.get("messages");
        var messagesCount = messages.size(0);
        if (messagesCount == 0) return;

        var newFilteredMessages = Value.emptyList();
        for (int i = 0; i < filteredMessagesCount; i++) {
            var messageIMaybe = filteredMessages.get(i);
            var messageI = messageIMaybe.isNull() ? V3439.EMPTY_TEXT : messageIMaybe;
            newFilteredMessages.add(filteredMessages.get(i).equals(Value.wrap(V3439.EMPTY_TEXT))
                    ? messageI : filteredMessages.get(i));
        }
        if (newFilteredMessages.equals(messages)) {
            data.remove("filtered_messages");
        } else {
            data.put("filtered_messages", newFilteredMessages);
        }
    }
}

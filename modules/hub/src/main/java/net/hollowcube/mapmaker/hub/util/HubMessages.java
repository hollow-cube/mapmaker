package net.hollowcube.mapmaker.hub.util;

import net.hollowcube.common.lang.MessagesBase;
import org.jetbrains.annotations.NotNull;

public enum HubMessages implements MessagesBase {

    // Commands (COMMAND_{NAME}_{STATUS})
    COMMAND_MAP_CREATE_SUCCESS("command.map.create.success"), // 0=map id, 1=slot
    COMMAND_MAP_CREATE_NO_SLOTS_AVAILABLE("command.map.create.no_slots_available"),
    COMMAND_MAP_CREATE_SLOT_NOT_AVAILABLE("command.map.create.slot_not_available"),

    COMMAND_MAP_ALTER_SUCCESS("command.map.alter.success"),
    COMMAND_MAP_ALTER_NO_CHANGE("command.map.alter.no_change"),

    COMMAND_MAP_DELETE_SUCCESS("command.map.delete.success"),

    ;

    private final String translationKey;

    HubMessages(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}

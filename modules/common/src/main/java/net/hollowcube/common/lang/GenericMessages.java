package net.hollowcube.common.lang;

import org.jetbrains.annotations.NotNull;

public enum GenericMessages implements MessagesBase {
    COMMAND_PLAYER_ONLY("command.generic.player_only"),
    COMMAND_UNKNOWN_ERROR("command.generic.unknown_error"),

    COMMAND_MAP_LEGACY_IMPORT_NO_PERMISSION("command.map.legacy.import.not_owner"),
    COMMAND_MAP_LEGACY_IMPORT_NOT_FOUND("command.map.legacy.import.invalid_id"),
    COMMAND_MAP_LEGACY_IMPORT_UNKNOWN_ERROR("command.map.legacy.import.failure"),
    COMMAND_MAP_LEGACY_IMPORT_SUCCESS("command.map.legacy.import.success"),

    COMMAND_HYPERCUBE_SUBSCRIPTION_INFO("command.hypercube.subscription_info");

    private final String translationKey;

    GenericMessages(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}

package net.hollowcube.common.lang;

import org.jetbrains.annotations.NotNull;

public enum GenericMessages implements MessagesBase {
    COMMAND_PLAYER_ONLY("command.generic.player_only"),
    COMMAND_UNKNOWN_ERROR("command.generic.unknown_error"),
    ;

    private final String translationKey;

    GenericMessages(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}

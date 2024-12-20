package net.hollowcube.mapmaker.map.hdb;

import net.hollowcube.terraform.util.MessageSet;
import org.jetbrains.annotations.NotNull;

public enum HdbMessages implements MessageSet {
    COMMAND_BASE64_NOT_A_PLAYER_HEAD("command.hdb.base64.not_a_player_head"),
    COMMAND_BASE64_NO_BLOCK("command.hdb.base64.no_block"),
    COMMAND_BASE64_NO_TEXTURE("command.hdb.base64.no_texture"),
    COMMAND_BASE64_RESULT("command.hdb.base64.result"),

    COMMAND_GIVE_NO_RESULT("command.hdb.give.no_result"),
    COMMAND_GIVE_RESULT("command.hdb.give.result"),

    ITEM_HDB_HEAD_NAME("item.hdb.head.name"),
    ;

    private final String key;

    HdbMessages(@NotNull String key) {
        this.key = key;
    }

    @Override
    public @NotNull String key() {
        return key;
    }
}

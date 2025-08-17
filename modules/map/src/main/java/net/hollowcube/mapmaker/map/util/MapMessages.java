package net.hollowcube.mapmaker.map.util;

import net.hollowcube.common.lang.MessagesBase;
import org.jetbrains.annotations.NotNull;

public enum MapMessages implements MessagesBase {
    // Checkpoints
    CHECKPOINT_REACHED("play.checkpoint.reached"),

    // Commands
    COMMAND_CLEAR_INVENTORY_SUCCESS("command.clear_inventory.success"),
    COMMAND_FLY_ENABLED("command.fly.enabled"),
    COMMAND_FLY_DISABLED("command.fly.disabled"),
    COMMAND_FLY_SPEED_CHANGED("command.flyspeed.changed"),

    COMMAND_GIVE_UNKNOWN_ITEM("command.give.unknown_item"),
    COMMAND_GIVE_NOT_ENOUGH_SPACE("command.give.not_enough_space"),
    COMMAND_GIVE_SUCCESS("command.give.success"),

    COMMAND_SETSPAWN_SUCCESS("command.set_spawn.success"),

    COMMAND_SETPRECISECOORDS_BEGIN("command.set_precise_coords.begin"),
    COMMAND_SETPRECISECOORDS_NO_TARGET("command.set_precise_coords.no_target"),
    COMMAND_SETPRECISECOORDS_SUCCESS("command.set_precise_coords.success"),

    // Misc
    SCHEMATIC_UPLOAD_SUCCESS("map.schematic.upload.success"),
    ;

    private final String translationKey;

    MapMessages(String translationKey) {
        this.translationKey = translationKey;
    }

    @Override
    public @NotNull String translationKey() {
        return translationKey;
    }
}

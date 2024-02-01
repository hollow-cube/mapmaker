package net.hollowcube.terraform.util;

import org.jetbrains.annotations.NotNull;

public enum Messages implements MessageSet {
    GENERIC_BLOCKS_CHANGED("terraform.generic.set"),
    GENERIC_NO_SELECTION("terraform.generic.no_selection"),
    GENERIC_NO_CLIPBOARD("terraform.generic.no_clipboard"),
    REGION_NOT_CUBOID("terraform.selection.not_cuboid"),

    HISTORY_UNDO("terraform.history.undo"),
    HISTORY_REDO("terraform.history.redo"),
    HISTORY_CLEARED("terraform.history.clear"),

    CLIPBOARD_CLEARED("terraform.clipboard.clear"),
    CLIPBOARD_FLIPPED("terraform.clipboard.flip"),
    CLIPBOARD_ROTATED("terraform.clipboard.rotate"),
    CLIPBOARD_COPY("terraform.copy"),
    CLIPBOARD_CUT("terraform.cut"),
    CLIPBOARD_PASTE("terraform.paste"),

    SELECTION_POS1_ALREADY_SET("terraform.pos1.already_set"),
    SELECTION_POS2_ALREADY_SET("terraform.pos2.already_set"),
    SELECTION_HPOS_NO_BLOCK("terraform.hpos.no_block"),
    SELECTION_RESHAPE_UNSUPPORTED("terraform.selection.reshape_unsupported"),
    SELECTION_CLEARED("terraform.selection.clear"),
    SELECTION_STACKED("terraform.selection.stack"), // 0=blocks changed
    SELECTION_MOVED("terraform.selection.move"), // 0=blocks changed

    SCHEM_LOADED("terraform.schem.load"), // 0=name
    SCHEM_SAVED("terraform.schem.save"), // 0=name
    SCHEM_DELETED("terraform.schem.delete"), // 0=name
    SCHEM_NOT_FOUND("terraform.schem.not_found"), // 0=name
    SCHEM_DUPLICATE("terraform.schem.duplicate"), // 0=name
    SCHEM_LIMIT_EXCEEDED("terraform.schem.limit_exceeded"),
    SCHEM_SIZE_LIMIT_EXCEEDED("terraform.schem.size_limit_exceeded"),

    TOOL_CREATED("terraform.tool.create"), // 0=name

    GENERIC_ENTITIES_UNSUPPORTED("terraform.generic.entities_unsupported"),
    GENERIC_BIOMES_UNSUPPORTED("terraform.generic.biomes_unsupported"),
    PASTE_ORIGINAL_POS_UNSUPPORTED("terraform.clipboard.paste.original_pos_unsupported"),
    ;

    private final String key;

    Messages(@NotNull String key) {
        this.key = key;
    }

    @Override
    public @NotNull String key() {
        return key;
    }

}

package net.hollowcube.compat.axiom;

import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.instance.block.Block;

import java.util.List;

public class AxiomAPI {
    
    public static final Component NAME = Component.text("Axiom", NamedTextColor.WHITE);
    public static final String CHANNEL = "axiom";
    public static final int API_VERSION = 8;
    public static final int BLUEPRINT_VERSION = 1;
    public static final int EMPTY_BLOCK_STATE = Block.STRUCTURE_VOID.stateId();
    public static final ListBinaryTag HIDDEN_MARKER_DATA = ListBinaryTag.listBinaryTag(BinaryTagTypes.STRING, List.of(
            StringBinaryTag.stringBinaryTag("name"), StringBinaryTag.stringBinaryTag("min"), StringBinaryTag.stringBinaryTag("max"),
            StringBinaryTag.stringBinaryTag("line_argb"), StringBinaryTag.stringBinaryTag("line_thickness"), StringBinaryTag.stringBinaryTag("face_argb")
    ));
    public static final List<String> RESERVED_MARKER_DATA = List.of("line_argb", "line_thickness", "face_argb", "axiom:hide", "axiom:modify");

}

package net.hollowcube.compat.axiom;

import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.instance.block.Block;

import java.util.List;

public class AxiomAPI {
    
    public static final Component NAME = Component.text("Axiom", NamedTextColor.WHITE);
    public static final String CHANNEL = "axiom";
    public static final int MIN_API_VERSION = 9;
    public static final int MAX_API_VERSION = 9;
    public static final int BLUEPRINT_VERSION = 1;
    public static final int EMPTY_BLOCK_STATE = Block.VOID_AIR.stateId();
    public static final List<String> HIDDEN_MARKER_KEYS = List.of(
            "name", "min", "max", "line_argb", "line_thickness", "face_argb"
    );
    public static final ListBinaryTag HIDDEN_MARKER_DATA = ListBinaryTag.builder()
            .add(HIDDEN_MARKER_KEYS.stream().map(StringBinaryTag::stringBinaryTag).toList())
            .build();
    public static final List<String> RESERVED_MARKER_DATA = List.of("line_argb", "line_thickness", "face_argb", "axiom:hide", "axiom:modify");

    private AxiomAPI() {
    }

}

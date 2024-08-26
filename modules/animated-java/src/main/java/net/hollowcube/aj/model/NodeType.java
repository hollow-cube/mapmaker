package net.hollowcube.aj.model;

import com.mojang.serialization.Codec;
import net.hollowcube.aj.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

public enum NodeType {
    BONE,
    STRUCT,
    CAMERA,
    LOCATOR,
    TEXT_DISPLAY,
    ITEM_DISPLAY,
    BLOCK_DISPLAY;

    public static final Codec<NodeType> CODEC = ExtraCodecs.enumString(NodeType.class);

    public @NotNull Codec<? extends ModelNode> nodeCodec() {
        return switch (this) {
            case BONE -> ModelNode.Bone.CODEC;
            case STRUCT -> ModelNode.Struct.CODEC;
            case CAMERA -> ModelNode.Camera.CODEC;
            case LOCATOR -> ModelNode.Locator.CODEC;
            case TEXT_DISPLAY -> ModelNode.TextDisplay.CODEC;
            case ITEM_DISPLAY -> ModelNode.ItemDisplay.CODEC;
            case BLOCK_DISPLAY -> ModelNode.BlockDisplay.CODEC;
        };
    }
}

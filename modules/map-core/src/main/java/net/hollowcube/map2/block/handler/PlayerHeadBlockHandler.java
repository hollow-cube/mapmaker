package net.hollowcube.map2.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.Collection;
import java.util.List;

import static net.hollowcube.map2.block.handler.BlockHandlerHelpers.applyItemData;

public class PlayerHeadBlockHandler implements BlockHandler {
    private static final Tag<NBT> SKULL_OWNER = Tag.NBT("SkullOwner");

    public static final NamespaceID ID = NamespaceID.from("minecraft:player_head");

    PlayerHeadBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public void onPlace(@NotNull BlockHandler.Placement placement) {
        if (!(placement instanceof PlayerPlacement p)) return;

        applyItemData(p); // Add the item NBT to the block
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(SKULL_OWNER);
    }

}

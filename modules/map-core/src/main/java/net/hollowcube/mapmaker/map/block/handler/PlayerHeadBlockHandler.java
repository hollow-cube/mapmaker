package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static net.hollowcube.mapmaker.map.block.handler.BlockHandlerHelpers.updateBlock;

public class PlayerHeadBlockHandler implements BlockHandler {
    public static final Tag<BinaryTag> PROFILE = Tag.NBT("profile");

    public static final NamespaceID ID = NamespaceID.from("minecraft:skull");

    public static @Nullable HeadProfile extractProfile(@NotNull Block block) {
        var profile = block.getTag(PROFILE);
        if (profile == null) return null;
        return ItemComponent.PROFILE.read(profile);
    }

    PlayerHeadBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public void onPlace(@NotNull BlockHandler.Placement placement) {
        if (!(placement instanceof PlayerPlacement p)) return;

        var itemStack = p.getPlayer().getItemInHand(p.getHand());
        var profile = itemStack.get(ItemComponent.PROFILE);
        if (profile == null) return;

        // Add the item NBT to the block
        updateBlock(p, CompoundBinaryTag.builder()
                .put("profile", ItemComponent.PROFILE.write(profile))
                .build());
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(PROFILE);
    }

}

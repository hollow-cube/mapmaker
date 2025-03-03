package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.nbt.BinaryTagSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PlayerHeadBlockHandler implements BlockHandler {
    public static final Tag<BinaryTag> PROFILE = Tag.NBT("profile");

    public static final Key ID = Key.key("minecraft:skull");

    public static @Nullable HeadProfile extractProfile(@NotNull Block block) {
        var profile = block.getTag(PROFILE);
        if (profile == null) return null;
        return ItemComponent.PROFILE.read(BinaryTagSerializer.Context.EMPTY, profile);
    }

    PlayerHeadBlockHandler() {
    }

    @Override
    public @NotNull Key getKey() {
        return ID;
    }

    @Override
    public byte getBlockEntityAction() {
        return 15;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(PROFILE);
    }

}

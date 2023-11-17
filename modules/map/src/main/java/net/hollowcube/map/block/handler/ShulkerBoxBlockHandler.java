package net.hollowcube.map.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

//todo can just be another form of chest block handler?
public class ShulkerBoxBlockHandler implements BlockHandler {

    public static final NamespaceID ID = NamespaceID.from("minecraft:shulker_box");
    public static final ShulkerBoxBlockHandler INSTANCE = new ShulkerBoxBlockHandler();

    private static final Tag<List<ItemStack>> ITEMS = Tag.ItemStack("Items").list().defaultValue(List.of());

    private ShulkerBoxBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(ITEMS);
    }

}

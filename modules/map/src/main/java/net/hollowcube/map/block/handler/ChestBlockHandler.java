package net.hollowcube.map.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ChestBlockHandler implements BlockHandler {

    public static final ChestBlockHandler CHEST = new ChestBlockHandler(NamespaceID.from("minecraft:chest"));
    public static final ChestBlockHandler TRAPPED_CHEST = new ChestBlockHandler(NamespaceID.from("minecraft:trapped_chest"));

    private final NamespaceID id;

    private ChestBlockHandler(@NotNull NamespaceID id) {
        this.id = id;
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return id;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of();
    }

}

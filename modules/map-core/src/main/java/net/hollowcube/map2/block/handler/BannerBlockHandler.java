package net.hollowcube.map2.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class BannerBlockHandler implements BlockHandler {

    private static final NamespaceID ID = NamespaceID.from("minecraft:banner");

    BannerBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        //todo patterns
        return List.of();
    }

}

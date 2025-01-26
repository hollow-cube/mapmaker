package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.ExtraTags;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.component.BannerPatterns;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class BannerBlockHandler implements BlockHandler {

    private static final NamespaceID ID = NamespaceID.from("minecraft:banner");
    public static final Tag<BannerPatterns> PATTERNS = ExtraTags.DataComponent("patterns", ItemComponent.BANNER_PATTERNS)
            .defaultValue(new BannerPatterns(List.of()));

    BannerBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(PATTERNS);
    }

}

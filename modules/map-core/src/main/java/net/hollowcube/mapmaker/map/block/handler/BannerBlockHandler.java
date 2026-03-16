package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.ExtraTags;
import net.kyori.adventure.key.Key;
import net.minestom.server.component.DataComponents;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.component.BannerPatterns;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class BannerBlockHandler implements BlockHandler {

    private static final Key ID = Key.key("minecraft:banner");
    public static final Tag<BannerPatterns> PATTERNS = ExtraTags.DataComponent("patterns", DataComponents.BANNER_PATTERNS)
        .defaultValue(new BannerPatterns(List.of()));

    BannerBlockHandler() {
    }

    @Override
    public Key getKey() {
        return ID;
    }

    @Override
    public Collection<Tag<?>> getBlockEntityTags() {
        return List.of(PATTERNS);
    }

}

package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.ExtraTags;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.component.PotDecorations;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DecoratedPotBlockHandler implements BlockHandler {

    private static final NamespaceID ID = NamespaceID.from("minecraft:decorated_pot");
    public static final Tag<PotDecorations> SHERDS = ExtraTags.DataComponent("sherds", ItemComponent.POT_DECORATIONS)
                    .defaultValue(PotDecorations.EMPTY);

    DecoratedPotBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(SHERDS);
    }

}

package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;

abstract class BaseBlockPlacementRule extends BlockPlacementRule {
    protected BaseBlockPlacementRule(@NotNull Block block) {
        super(block);
    }

    protected boolean canConnect(@NotNull Block block) {
        return !(BlockTags.LEAVES.contains(block.namespace()) ||
                block.id() == Block.BARRIER.id() ||
                block.id() == Block.CARVED_PUMPKIN.id() ||
                block.id() == Block.JACK_O_LANTERN.id() ||
                block.id() == Block.MELON.id() ||
                block.id() == Block.PUMPKIN.id() ||
                BlockTags.SHULKER_BOXES.contains(block.namespace()));
    }

    @Override
    public int maxUpdateDistance() {
        return 0;
    }
}

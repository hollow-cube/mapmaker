package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

public class TripwireHookPlacementRule extends FacingClickHorizontalPlacementRule {
    private static final String PROP_FACING = "facing";
    private static final String PROP_ATTACHED = "attached";

    public TripwireHookPlacementRule() {
        super(Block.TRIPWIRE_HOOK);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        var currentBlock = updateState.currentBlock();
        var facing = BlockFace.valueOf(currentBlock.getProperty(PROP_FACING).toUpperCase());
        var currentAttached = Boolean.parseBoolean(currentBlock.getProperty("attached"));

        var instance = updateState.instance();
        var neighbor = instance.getBlock(updateState.blockPosition().relative(facing), Block.Getter.Condition.TYPE);
        if (!currentAttached && (neighbor.id() == Block.TRIPWIRE.id() || neighbor.id() == Block.TRIPWIRE_HOOK.id()))
            return currentBlock.withProperty(PROP_ATTACHED, "true");
        else if (currentAttached && neighbor.id() != Block.TRIPWIRE.id() && neighbor.id() != Block.TRIPWIRE_HOOK.id())
            return currentBlock.withProperty(PROP_ATTACHED, "false");
        return currentBlock;
    }

}

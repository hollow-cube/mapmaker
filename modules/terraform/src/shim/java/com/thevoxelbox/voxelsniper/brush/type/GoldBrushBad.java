package com.thevoxelbox.voxelsniper.brush.type;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.thevoxelbox.voxelsniper.brush.type.performer.AbstractPerformerBrush;
import com.thevoxelbox.voxelsniper.performer.Performer;
import net.minestom.server.instance.block.Block;

public class GoldBrushBad extends AbstractPerformerBrush {
    private final EditSession editSession;

    public GoldBrushBad(EditSession editSession) {
        this.editSession = editSession;
        performer = new Performer() {
            @Override public void perform(EditSession s, int x, int y, int z, BlockState block) {
                s.setBlock(BlockVector3.at(x, y, z), (Pattern) new BlockState(Block.GOLD_BLOCK));
            }
        };
    }

    @Override
    public EditSession getEditSession() {
        return editSession;
    }

    @Override
    public BlockState getBlock(BlockVector3 pos) {
        return new BlockState(Block.GOLD_BLOCK); //todo
    }
}

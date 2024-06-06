package com.thevoxelbox.voxelsniper.sniper.snipe;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.thevoxelbox.voxelsniper.brush.Brush;
import com.thevoxelbox.voxelsniper.brush.type.GoldBrushBad;
import com.thevoxelbox.voxelsniper.performer.Performer;
import com.thevoxelbox.voxelsniper.sniper.Sniper;
import com.thevoxelbox.voxelsniper.sniper.snipe.message.SnipeMessageSender;
import com.thevoxelbox.voxelsniper.sniper.snipe.message.SnipeMessenger;
import com.thevoxelbox.voxelsniper.sniper.toolkit.ToolkitProperties;

public record Snipe(
        EditSession editSession,
        Sniper sniper
) {

    public EditSession getEditSession() {
        return editSession;
    }

    public Brush getBrush() {
        return new GoldBrushBad(getEditSession()); //todo
    }

    public Sniper getSniper() {
        return sniper;
    }

    public ToolkitProperties getToolkitProperties() {
        return new ToolkitProperties();
    }

    public SnipeMessenger createMessenger() {
        return new SnipeMessenger();
    }

    public SnipeMessageSender createMessageSender() {
        return new SnipeMessageSender();
    }

    public Performer getPerformer() {
        return new Performer() {

            @Override
            public void perform(EditSession s, int x, int y, int z, BlockState block) {
                //todo is this the right behavior?
                editSession.setBlock(BlockVector3.at(x, y, z), (BlockStateHolder) block);
            }
        };
    }
}

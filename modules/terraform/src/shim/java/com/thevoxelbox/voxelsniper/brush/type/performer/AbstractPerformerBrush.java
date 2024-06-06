package com.thevoxelbox.voxelsniper.brush.type.performer;

import com.thevoxelbox.voxelsniper.brush.type.AbstractBrush;
import com.thevoxelbox.voxelsniper.performer.Performer;
import com.thevoxelbox.voxelsniper.sniper.snipe.Snipe;

public class AbstractPerformerBrush extends AbstractBrush {

    protected Performer performer;

    // Method impls


    @Override
    public void wrappedHandleArrowAction(Snipe snipe) {
        this.performer = snipe.getPerformer();
        super.wrappedHandleArrowAction(snipe);
    }

    @Override
    public void wrappedHandleGunpowderAction(Snipe snipe) {
        this.performer = snipe.getPerformer();
        super.wrappedHandleGunpowderAction(snipe);
    }
}

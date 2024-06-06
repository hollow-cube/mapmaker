package com.thevoxelbox.voxelsniper.brush.type;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.thevoxelbox.voxelsniper.brush.Brush;
import com.thevoxelbox.voxelsniper.sniper.snipe.Snipe;
import com.thevoxelbox.voxelsniper.sniper.toolkit.ToolAction;
import net.minestom.server.utils.validate.Check;

public abstract class AbstractBrush implements Brush {

    protected Snipe snipe;
    protected EditSession editSession;

    private BlockVector3 targetBlock;
    private BlockVector3 lastBlock;

    public void wrappedHandleArrowAction(Snipe snipe) {
        this.snipe = snipe;
        this.editSession = snipe.getEditSession();
        handleArrowAction(snipe);
    }

    public void wrappedHandleGunpowderAction(Snipe snipe) {
        this.snipe = snipe;
        this.editSession = snipe.getEditSession();
        handleGunpowderAction(snipe);
    }

    public void handleArrowAction(Snipe snipe) {

    }

    public void handleGunpowderAction(Snipe snipe) {

    }

    public void loadProperties() {

    }

    public void perform(Snipe snipe, ToolAction action, EditSession editSession, BlockVector3 targetBlock, BlockVector3 lastBlock) {
        this.snipe = snipe;
        this.editSession = editSession;
        this.targetBlock = targetBlock;
        this.lastBlock = lastBlock;
        switch (action) {
            case ARROW:
                handleArrowAction(snipe);
                break;
            case GUNPOWDER:
                handleGunpowderAction(snipe);
                break;
        }
    }

    public void setBlockData(BlockVector3 pos, BlockState blockState) {
        getEditSession().setBlock(pos, (BlockStateHolder) blockState); //todo this does an istile check which could be relevant https://github.com/IntellectualSites/fastasyncvoxelsniper/blob/ea6c97418c1760d526621fc1901ea48b9bc0e809/src/main/java/com/thevoxelbox/voxelsniper/brush/type/AbstractBrush.java#L225
    }

    public void setBlockData(int x, int y, int z, BlockState blockState) {
        setBlockData(BlockVector3.at(x, y, z), blockState);
    }


    public EditSession getEditSession() {
        return editSession;
    }

    public BlockVector3 getTargetBlock() {
        var blockPos = snipe.getSniper().player().getTargetBlockPosition(1000);
        Check.notNull(blockPos, "Target block is null");
        return new BlockVector3(blockPos);
    }

    public int clampY(int y) {
        int clampedY = y;
        int minHeight = getEditSession().getMinY();
        if (clampedY < minHeight) {
            clampedY = minHeight;
        } else {
            int maxHeight = getEditSession().getMaxY();
            if (clampedY > maxHeight) {
                clampedY = maxHeight;
            }
        }
        return clampedY;
    }

    public BlockState clampY(BlockVector3 position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        return clampY(x, y, z);
    }

    public BlockState clampY(int x, int y, int z) {
        return getBlock(x, clampY(y), z);
    }

    public BlockState getBlock(BlockVector3 position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        return getBlock(x, y, z);
    }

    public BlockState getBlock(int x, int y, int z) {
        return getEditSession().getBlock(x, y, z);
    }

    public BlockType getBlockType(BlockVector3 position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        return getBlockType(x, y, z);
    }

    public BlockType getBlockType(int x, int y, int z) {
        return getEditSession().getBlockType(x, y, z);
    }

    public BlockVector3 getLastBlock() {
        return this.lastBlock;
    }

    public void setLastBlock(BlockVector3 lastBlock) {
        this.lastBlock = lastBlock;
    }
}

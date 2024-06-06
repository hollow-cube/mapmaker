package com.sk89q.worldedit;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.WorldImpl;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class EditSession implements Extent {
    private final Instance instance;
    private ReorderMode reorderMode = ReorderMode.FAST;

    public enum ReorderMode {
        FAST
    }

    public EditSession(Instance instance) {
        this.instance = instance;
    }

    public int getMinY() {
        return instance.getCachedDimensionType().minY();
    }

    public int getMaxY() {
        return instance.getCachedDimensionType().maxY();
    }

    public BlockState getBlock(int x, int y, int z) {
        return new BlockState(instance.getBlock(x, y, z));
    }

    public BlockState getBlock(BlockVector3 pos) {
        return getBlock(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockType getBlockType(int x, int y, int z) {
        return new BlockType(instance.getBlock(x, y, z));
    }

    public Operation commit() {
        System.out.println("EditSession.commit");
        return new Operation() {
        };
    }

    public void setReorderMode(ReorderMode reorderMode) {
        this.reorderMode = reorderMode;
    }

    public boolean setBlock(BlockVector3 pos, BlockStateHolder blockStateHolder) {
        Block old = instance.getBlock(pos.getX(), pos.getY(), pos.getZ()); //todo i love 2 block gets and a set
        instance.setBlock(pos.getX(), pos.getY(), pos.getZ(), ((BlockState) blockStateHolder).block());
        return !instance.getBlock(pos.getX(), pos.getY(), pos.getZ()).equals(old);
    }

    public boolean setBlock(BlockVector3 pos, Pattern pattern) {
        return setBlock(pos, (BlockStateHolder) pattern.apply(pos));
    }

    public World getWorld() {
        return new WorldImpl(instance);
    }

    private SideEffectSet sideEffectSet = new SideEffectSet();

    public SideEffectSet getSideEffectApplier() {
        return sideEffectSet;
    }

    public void setSideEffectApplier(SideEffectSet sideEffectSet) {
        this.sideEffectSet = sideEffectSet;
    }

    public BlockVector3 getMinimumPoint() {
        var border = instance.getWorldBorder();
        return new BlockVector3(
                (int) (border.centerX() - (border.diameter() / 2)),
                getMinY(),
                (int) (border.centerZ() - (border.diameter() / 2))
        );
    }

    public BlockVector3 getMaximumPoint() {
        var border = instance.getWorldBorder();
        return new BlockVector3(
                (int) (border.centerX() + (border.diameter() / 2)),
                getMaxY(),
                (int) (border.centerZ() + (border.diameter() / 2))
        );
    }

    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; --y) {
            if (isMotionBlocking(getBlock(x, y, z).block())) { //todo confirm that the W/E definition of motion blocking is the same as ours https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/5714a526756e1819b1598f31211e26b48cdd217d/worldedit-core/src/main/java/com/sk89q/worldedit/EditSession.java#L936C9-L941C10
                return y;
            }
            //FAWE end
        }
        return minY;
    }

    private static boolean isMotionBlocking(@NotNull Block block) {
        return block.id() != Block.COBWEB.id() && block.id() != Block.BAMBOO_SAPLING.id() && block.isSolid();
    }
}

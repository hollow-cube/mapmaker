package com.sk89q.worldedit.world.block;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import net.minestom.server.instance.block.Block;

public record BlockState(Block block) implements BlockStateHolder, Pattern {

    public BlockType getBlockType() {
        return new BlockType(block); //todo
    }

    public BlockMaterial getMaterial() {
        return () -> block;
    }

    public <T> BlockState with(Property<T> property, T value) {
        return new BlockState(block.withProperty(property.getKey(), property.serialize(value)));
    }

    public <T> T getState(Property<T> property) {
        return property.deserialize(block.getProperty(property.getKey()));
    }

    @Override
    public BlockState apply(BlockVector3 pos) {
        return this;
    }
}

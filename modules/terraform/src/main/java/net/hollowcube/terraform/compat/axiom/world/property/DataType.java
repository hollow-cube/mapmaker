package net.hollowcube.terraform.compat.axiom.world.property;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

public sealed interface DataType<T> permits
        DataTypes.BooleanType,
        DataTypes.IntegerType,
        DataTypes.StringType,
        DataTypes.MaterialType,
        DataTypes.BlockType,
        DataTypes.EmptyType {

    DataType<Boolean> BOOLEAN = new DataTypes.BooleanType();
    DataType<Integer> INTEGER = new DataTypes.IntegerType();
    DataType<String> STRING = new DataTypes.StringType();
    DataType<Material> MATERIAL = new DataTypes.MaterialType();
    DataType<Block> BLOCK = new DataTypes.BlockType();
    DataType<Void> EMPTY = new DataTypes.EmptyType();

    int typeId();

    byte[] serialize(T value);

    T deserialize(byte[] value);

}

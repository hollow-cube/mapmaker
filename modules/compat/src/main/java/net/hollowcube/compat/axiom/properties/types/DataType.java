package net.hollowcube.compat.axiom.properties.types;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.Unit;

public record DataType<T>(int id, NetworkBuffer.Type<T> codec) {

    public static final DataType<Boolean> BOOLEAN = new DataType<>(0, NetworkBuffer.BOOLEAN);
    public static final DataType<Integer> INTEGER = new DataType<>(1, NetworkBuffer.VAR_INT);
    public static final DataType<String> STRING = new DataType<>(2, NetworkBuffer.STRING);
    public static final DataType<Material> MATERIAL = new DataType<>(3, Material.NETWORK_TYPE);
    public static final DataType<Block> BLOCK = new DataType<>(4, Block.ID_NETWORK_TYPE);
    public static final DataType<Unit> UNIT = new DataType<>(5, NetworkBuffer.UNIT);

}

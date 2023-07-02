package net.hollowcube.terraform.blueprint;

import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static net.minestom.server.network.NetworkBuffer.*;

/**
 * Basically a schematic, but serialized slightly differently and can differentiate between
 * an unspecified block and an air block. Unspecified blocks are never set during apply, but
 * air blocks replace the block in place.
 */
@SuppressWarnings("UnstableApiUsage")
public record Blueprint(
        byte version,
        Point size,
        Point offset,
        Block[] blockPalette,
        long[] blockData
) {
    public static final byte VERSION = 1;

    public static @NotNull BlueprintBuilder builder() {
        return new BlueprintBuilder();
    }

    public static @NotNull Blueprint read(byte[] data) {
        var buffer = new NetworkBuffer(ByteBuffer.wrap(data));
        var version = buffer.read(BYTE);

        var size = buffer.read(VECTOR3);
        var offset = buffer.read(VECTOR3);

        int numberOfBlocks = buffer.read(VAR_INT);
        var blockPalette = new Block[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; i++) {
            var blockState = buffer.read(STRING);
            var block = ArgumentBlockState.staticParse(blockState);
        }

//        return new Blueprint(version, size, offset, );
        throw new UnsupportedOperationException();
    }

    public byte @NotNull[] write() {
        throw new UnsupportedOperationException();
    }

}

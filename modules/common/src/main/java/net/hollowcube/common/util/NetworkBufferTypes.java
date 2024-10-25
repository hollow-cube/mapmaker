package net.hollowcube.common.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class NetworkBufferTypes {

    public static final NetworkBuffer.Type<Point> VECTOR3VI = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, Point value) {
            buffer.write(NetworkBuffer.VAR_INT, value.blockX());
            buffer.write(NetworkBuffer.VAR_INT, value.blockY());
            buffer.write(NetworkBuffer.VAR_INT, value.blockZ());
        }

        @Override
        public Point read(@NotNull NetworkBuffer buffer) {
            int x = buffer.read(NetworkBuffer.VAR_INT);
            int y = buffer.read(NetworkBuffer.VAR_INT);
            int z = buffer.read(NetworkBuffer.VAR_INT);
            return new BlockVec(x, y, z);
        }
    };

    public static final NetworkBuffer.Type<BlockFace> BLOCK_FACE = NetworkBuffer.DIRECTION
            .transform(BlockFace::fromDirection, BlockFace::toDirection);

    public static final NetworkBuffer.Type<CompoundBinaryTag> NBT_COMPOUND_OR_END = NetworkBuffer.NBT.transform(
            tag -> tag instanceof CompoundBinaryTag compound ? compound : CompoundBinaryTag.empty(),
            compound -> compound.keySet().isEmpty() ? EndBinaryTag.endBinaryTag() : compound);

    public static <T> NetworkBuffer.@NotNull Type<T> readOnly(@NotNull Function<NetworkBuffer, T> reader) {
        return new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, T value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public T read(@NotNull NetworkBuffer buffer) {
                return reader.apply(buffer);
            }
        };
    }

    public static <T> NetworkBuffer.@NotNull Type<T> writeOnly(@NotNull BiConsumer<NetworkBuffer, T> writer) {
        return new NetworkBuffer.Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, T value) {
                writer.accept(buffer, value);
            }

            @Override
            public T read(@NotNull NetworkBuffer buffer) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static final NetworkBuffer.Type<@Nullable Point> OPT_VECTOR3 = NetworkBuffer.VECTOR3.optional();
}

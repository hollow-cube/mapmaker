package net.hollowcube.compat.api.packet;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.EndBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBuffer.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class ExtraNetworkBuffers {

    public static final Type<@Nullable CompoundBinaryTag> OPTIONAL_COMPOUND_TAG = NetworkBuffer.NBT.transform(
            nbt -> nbt instanceof EndBinaryTag ? null : (CompoundBinaryTag) nbt,
            nbt -> nbt == null ? EndBinaryTag.endBinaryTag() : nbt
    );

    public static final Type<Key> KEY = NetworkBuffer.STRING.transform(Key::key, Key::asString);

    public static <C extends Collection<T>, T> Type<C> collection(Type<T> elementType, Int2ObjectFunction<C> factory) {
        return new Type<>() {
            @Override
            public void write(@NotNull NetworkBuffer buffer, C value) {
                if (value == null) {
                    buffer.write(NetworkBuffer.VAR_INT, 0);
                } else {
                    buffer.write(NetworkBuffer.VAR_INT, value.size());
                    for (T element : value) {
                        elementType.write(buffer, element);
                    }
                }
            }

            @Override
            public C read(@NotNull NetworkBuffer buffer) {
                int size = buffer.read(NetworkBuffer.VAR_INT);
                C collection = factory.get(size);
                for (int i = 0; i < size; i++) {
                    collection.add(elementType.read(buffer));
                }
                return collection;
            }
        };
    }
}

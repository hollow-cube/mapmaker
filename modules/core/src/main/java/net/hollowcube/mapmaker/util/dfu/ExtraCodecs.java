package net.hollowcube.mapmaker.util.dfu;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ExtraCodecs {

    public static final Codec<PotionEffect> POTION_EFFECT = Codec.STRING.xmap(PotionEffect::fromNamespaceId, PotionEffect::name);

    public static final Codec<Point> POINT = Codec.DOUBLE.listOf().xmap(list -> {
        Check.stateCondition(list.size() != 3, "Expected 3 doubles, got " + list.size());
        return new Vec(list.get(0), list.get(1), list.get(2));
    }, point -> List.of(point.x(), point.y(), point.z()));

    public static final Codec<Pos> POS = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Pos::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Pos::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Pos::z),
            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Pos::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Pos::pitch)
    ).apply(i, Pos::new));

    /**
     * Admittedly this is kinda disgusting, but it writes the item map as a base64
     * string of a network-encoded map int->nbt.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Codec<Map<Integer, ItemStack>> ITEM_STACK_MAP_AS_BASE64 = Codec.STRING.xmap(
            s -> ProtocolUtil.readMap(
                    new NetworkBuffer(ByteBuffer.wrap(Base64.getDecoder().decode(s))),
                    NetworkBuffer.VAR_INT, buffer -> ItemStack.fromItemNBT((NBTCompound) buffer.read(NetworkBuffer.NBT))),
            items -> Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> ProtocolUtil.writeMap(
                    b, NetworkBuffer.VAR_INT,
                    (item, buffer) -> buffer.write(NetworkBuffer.NBT, item.toItemNBT()), items)
            ))
    );

    public static <T> @NotNull Codec<T> Lazy(Supplier<Codec<T>> supplier) {
        return new Codec<T>() {
            private Codec<T> codec = null;

            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                if (codec == null) codec = supplier.get();
                return codec.decode(ops, input);
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                if (codec == null) codec = supplier.get();
                return codec.encode(input, ops, prefix);
            }
        };
    }
}

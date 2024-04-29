package net.hollowcube.mapmaker.util.dfu;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
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

    public static final Codec<Material> MATERIAL = Codec.STRING
            .xmap(Material::fromNamespaceId, Material::name);

    // Enum as ordinal integer
    public static <T extends Enum<T>> @NotNull Codec<T> EnumI(@NotNull Class<T> enumClass) {
        var values = enumClass.getEnumConstants();
        return Codec.INT.xmap(ord -> values[ord], Enum::ordinal);
    }

    /**
     * Admittedly this is kinda disgusting, but it writes the item map as a base64
     * string of a network-encoded map int->nbt.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Codec<Map<Integer, ItemStack>> ITEM_STACK_MAP_AS_BASE64 = Codec.STRING.xmap(
            s -> {
                //todo HUGE TODO FOR ME TO READ WHEN I REOPEN THIS IN THE MORNING
                // HELLO FUTURE ME -- I AM SORRY
                // YOU NEED TO MOVE THE PLAY AND BUILD SAVE STATES INTO THE MAP (non-core ideally) MODULE
                // IT WILL PROBABLY BE A GIGA PAIN, BUT SUCKS TO SUCK. THE ISSUE HERE IS THAT WE CANNOT USE
                // MapWorld OR MCDataConverter (though that could be fixed by depending on it, but id rather not).

                // This is gross because we need to handle backwards compat to before the data version was encoded here
                var buffer = new NetworkBuffer(ByteBuffer.wrap(Base64.getDecoder().decode(s)));
                int dataVersionOrLength = buffer.read(NetworkBuffer.VAR_INT);
                int dataVersion = dataVersionOrLength > 99 ? dataVersionOrLength : MapWorld.DATA_VERSION;
                int length = dataVersionOrLength > 99 ? buffer.read(NetworkBuffer.VAR_INT) : dataVersionOrLength;

                var entries = new HashMap<Integer, ItemStack>(length);
                for (int i = 0; i < length; i++) {
                    int key = buffer.read(NetworkBuffer.VAR_INT);
                    var compound = (CompoundBinaryTag) buffer.read(NetworkBuffer.NBT);
                    if (dataVersion < MapWorld.DATA_VERSION) {
                        // Convert the item version to the latest version
                        compound = MCDataConverter.convertTag(MCTypeRegistry.ITEM_STACK, compound, dataVersion, MapWorld.DATA_VERSION);
                    }
                    entries.put(key, ItemStack.NBT_TYPE.read(compound));
                }
                return entries;
            },
            items -> Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(b -> ProtocolUtil.writeMap(
                    b, NetworkBuffer.VAR_INT,
                    (item, buffer) -> {
                        buffer.write(NetworkBuffer.VAR_INT, MapWorld.DATA_VERSION);
                        buffer.write(NetworkBuffer.NBT, item.toItemNBT());
                    }, items)
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

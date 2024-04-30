package net.hollowcube.mapmaker.map.util;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EvenMoreCodecs {


    /**
     * Admittedly this is kinda disgusting, but it writes the item map as a base64
     * string of a network-encoded map int->nbt.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Codec<Map<Integer, ItemStack>> ITEM_STACK_MAP_AS_BASE64 = Codec.STRING.xmap(
            s -> {
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
            items -> Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, MapWorld.DATA_VERSION);
                ProtocolUtil.writeMap(buffer, NetworkBuffer.VAR_INT,
                        (item, b) -> b.write(NetworkBuffer.NBT, item.toItemNBT()), items);
            }))
    );

    private record VersionedPosBlockMap(int dataVersion, Map<Long, String> map) {

        private static final Codec<VersionedPosBlockMap> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("dataVersion").forGetter(VersionedPosBlockMap::dataVersion),
                Codec.unboundedMap(Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.STRING).fieldOf("map").forGetter(VersionedPosBlockMap::map)
        ).apply(i, VersionedPosBlockMap::new));

        public static VersionedPosBlockMap forLatest(@NotNull Map<Long, Block> blockMap) {
            var stringMap = new HashMap<Long, String>(blockMap.size());
            for (var entry : blockMap.entrySet())
                stringMap.put(entry.getKey(), BlockUtil.toString(entry.getValue()));
            return new VersionedPosBlockMap(MapWorld.DATA_VERSION, stringMap);
        }

        public @NotNull Map<Long, Block> toBlockMap() {
            var blockMap = new HashMap<Long, Block>(map.size());
            for (var entry : map.entrySet()) {
                String blockState = entry.getValue();
                if (dataVersion < MapWorld.DATA_VERSION) {
                    blockState = (String) MCDataConverter.convert(MCTypeRegistry.FLAT_BLOCK_STATE, blockState, dataVersion, MapWorld.DATA_VERSION);
                }
                blockMap.put(entry.getKey(), BlockUtil.fromString(blockState));
            }
            return blockMap;
        }
    }

    public static final Codec<Map<Long, Block>> VERSIONED_POS_BLOCK_MAP = VersionedPosBlockMap.CODEC
            .xmap(VersionedPosBlockMap::toBlockMap, VersionedPosBlockMap::forLatest);

}

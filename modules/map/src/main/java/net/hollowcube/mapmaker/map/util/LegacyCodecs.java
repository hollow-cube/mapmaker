package net.hollowcube.mapmaker.map.util;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.serialization.Codec;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>This will be kept for backwards compat for the forseeable future, but no new usages or content should be added here.</p>
 *
 * <p>Data upgrades on play and build states is done when reading the save state (see {@link HCTypeRegistry#EDIT_STATE} and {@link HCTypeRegistry#PLAY_STATE}).</p>
 *
 * <p>Also, future usages of {@link ItemStack} should use {@link ExtraCodecs#ITEM_STACK}, which supports transparent DFU conversion.</p>
 */
@Deprecated
public class LegacyCodecs {

    /**
     * Admittedly this is kinda disgusting, but it writes the item map as a base64
     * string of a network-encoded map int->nbt.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static final Codec<Map<Integer, ItemStack>> ITEM_STACK_MAP_AS_BASE64 = Codec.STRING.xmap(
            s -> {
                // This is gross because we need to handle backwards compat to before the data version was encoded here
                var buffer = NetworkBuffer.wrap(Base64.getDecoder().decode(s), 0, 0);
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

}

package net.hollowcube.mapmaker.map.util;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.mojang.serialization.Codec;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;

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
            items -> Base64.getEncoder().encodeToString(NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, MapWorld.DATA_VERSION);
                ProtocolUtil.writeMap(buffer, NetworkBuffer.VAR_INT,
                        (item, b) -> b.write(NetworkBuffer.NBT, item.toItemNBT()), items);
            }))
    );
}

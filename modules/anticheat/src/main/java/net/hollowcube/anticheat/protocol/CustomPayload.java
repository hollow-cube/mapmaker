package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/// `common custom_payload`. Only `minecraft:brand` is interpreted (it becomes the trace header's
/// client brand); everything else keeps its bytes.
public sealed interface CustomPayload extends Packet permits S2CCustomPayload, C2SCustomPayload {

    String BRAND_CHANNEL = "minecraft:brand";
    String REGISTER_CHANNEL = "minecraft:register";
    String UNREGISTER_CHANNEL = "minecraft:unregister";

    String channel();

    byte[] payload();

    /// The channel names a `minecraft:register` or `minecraft:unregister` payload carries — UTF-8
    /// names separated by NUL, as the client's `ClientCommonPacketListenerImpl` writes them — and
    /// nothing for any other channel. Mods that talk to the server register theirs at join.
    default List<String> channels() {
        if (!REGISTER_CHANNEL.equals(channel()) && !UNREGISTER_CHANNEL.equals(channel())) return List.of();
        var names = new ArrayList<String>();
        for (var name : new String(payload(), StandardCharsets.UTF_8).split("\0")) if (!name.isEmpty()) names.add(name);
        return names;
    }

    /// The brand string when [#channel()] is [#BRAND_CHANNEL] and the payload parses, else null.
    default @Nullable String brand() {
        if (!BRAND_CHANNEL.equals(channel())) return null;
        try {
            return new ByteReader(payload()).utf();
        } catch (ProtocolException _) {
            return null;
        }
    }
}

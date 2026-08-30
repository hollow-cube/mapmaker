package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

/// `common custom_payload`. Only `minecraft:brand` is interpreted (it becomes the trace header's
/// client brand); everything else keeps its bytes.
public sealed interface CustomPayload extends Packet permits S2CCustomPayload, C2SCustomPayload {

    String BRAND_CHANNEL = "minecraft:brand";

    String channel();

    byte[] payload();

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

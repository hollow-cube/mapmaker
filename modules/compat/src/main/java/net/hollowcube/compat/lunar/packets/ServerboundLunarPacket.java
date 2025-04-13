package net.hollowcube.compat.lunar.packets;

import net.hollowcube.compat.api.packet.ServerboundModPacket;

// For lunar to advertise itself we have to ensure we register the channels
// Rather than send another register message, we'll just register some dummy handlers
// Who knows, maybe they'll be useful in the future if lunar add more serverbound packets
public record ServerboundLunarPacket() implements ServerboundModPacket<ServerboundLunarPacket> {
    public static final Type<ServerboundLunarPacket> LUNAR_APOLLO_TYPE = Type.of(
            "lunar",
            "apollo",
            buffer -> new ServerboundLunarPacket()
    );

    public static final Type<ServerboundLunarPacket> APOLLO_JSON_TYPE = Type.of(
            "apollo",
            "json",
            buffer -> new ServerboundLunarPacket()
    );

    @Override
    public Type<ServerboundLunarPacket> getType() {
        return null;
    }
}

package net.hollowcube.compat.vanilla.packets;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;

public record ServerboundBrandPacket(String brand) implements ServerboundModPacket<ServerboundBrandPacket> {

    public static final Type<ServerboundBrandPacket> TYPE = Type.of(
            "minecraft", "brand",
            NetworkBuffer.STRING.transform(ServerboundBrandPacket::new, ServerboundBrandPacket::brand)
    );

    @Override
    public Type<ServerboundBrandPacket> getType() {
        return TYPE;
    }
}

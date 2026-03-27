package net.hollowcube.compat.noxesium.packets.v2;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ServerboundClientInformationPacket(
    byte version,
    String versionString
) implements ServerboundModPacket<ServerboundClientInformationPacket> {

    public static final Type<ServerboundClientInformationPacket> TYPE = Type.of(
        "noxesium-v2", "client_info",
        NetworkBufferTemplate.template(
            NetworkBuffer.BYTE, ServerboundClientInformationPacket::version,
            NetworkBuffer.STRING, ServerboundClientInformationPacket::versionString,
            ServerboundClientInformationPacket::new
        )
    );

    @Override
    public Type<ServerboundClientInformationPacket> getType() {
        return TYPE;
    }
}

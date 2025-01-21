package net.hollowcube.compat.noxesium.packets;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ServerboundClientInformationPacket(byte version, String versionString) implements ServerboundModPacket<ServerboundClientInformationPacket> {

    public static final Type<ServerboundClientInformationPacket> TYPE = Type.of(
            NoxesiumAPI.CHANNEL, "client_info",
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

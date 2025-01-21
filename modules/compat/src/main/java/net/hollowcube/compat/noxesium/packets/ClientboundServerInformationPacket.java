package net.hollowcube.compat.noxesium.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.minestom.server.network.NetworkBuffer;

public record ClientboundServerInformationPacket(int version) implements ClientboundModPacket<ClientboundServerInformationPacket> {

    public static final Type<ClientboundServerInformationPacket> TYPE = Type.of(
            NoxesiumAPI.CHANNEL, "server_info",
            NetworkBuffer.VAR_INT.transform(ClientboundServerInformationPacket::new, ClientboundServerInformationPacket::version)
    );

    @Override
    public Type<ClientboundServerInformationPacket> getType() {
        return TYPE;
    }
}

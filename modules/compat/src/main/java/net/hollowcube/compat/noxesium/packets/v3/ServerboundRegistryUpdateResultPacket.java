package net.hollowcube.compat.noxesium.packets.v3;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ServerboundRegistryUpdateResultPacket(
    int registry,
    int[] missing
) implements ServerboundModPacket<ServerboundRegistryUpdateResultPacket> {

    public static final Type<ServerboundRegistryUpdateResultPacket> TYPE = Type.of(
        "noxesium-v3", "serverbound_registry_update_result-p1",
        NetworkBufferTemplate.template(
            NetworkBuffer.VAR_INT, ServerboundRegistryUpdateResultPacket::registry,
            NetworkBuffer.VAR_INT_ARRAY, ServerboundRegistryUpdateResultPacket::missing,
            ServerboundRegistryUpdateResultPacket::new
        )
    );

    @Override
    public Type<ServerboundRegistryUpdateResultPacket> getType() {
        return TYPE;
    }
}

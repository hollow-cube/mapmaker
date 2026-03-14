package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ExtraNetworkBuffers;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.properties.WorldProperty;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomClientboundSetWorldPropertyPacket(
    Key property,
    int type,
    byte[] data
) implements AxiomClientboundModPacket<AxiomClientboundSetWorldPropertyPacket> {

    public static final Type<AxiomClientboundSetWorldPropertyPacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "set_world_property",
        NetworkBufferTemplate.template(
            ExtraNetworkBuffers.KEY, AxiomClientboundSetWorldPropertyPacket::property,
            NetworkBuffer.VAR_INT, AxiomClientboundSetWorldPropertyPacket::type,
            NetworkBuffer.BYTE_ARRAY, AxiomClientboundSetWorldPropertyPacket::data,
            AxiomClientboundSetWorldPropertyPacket::new
        )
    );

    public static <T> AxiomClientboundSetWorldPropertyPacket of(WorldProperty<T> property, T value) {
        var id = property.id();
        var type = property.widget().id();
        var data = NetworkBuffer.makeArray(property.widget().type().codec(), value);
        return new AxiomClientboundSetWorldPropertyPacket(id, type, data);
    }

    @Override
    public Type<AxiomClientboundSetWorldPropertyPacket> getType() {
        return TYPE;
    }
}

package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.hollowcube.compat.axiom.properties.PropertyCategory;
import net.hollowcube.compat.axiom.properties.WorldProperty;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record AxiomClientboundRegisterWorldPropertiesPacket(
        @NotNull Player player,
        @NotNull Map<PropertyCategory, List<WorldProperty<?>>> properties
) implements ClientboundModPacket<AxiomClientboundRegisterWorldPropertiesPacket> {

    public static final Type<AxiomClientboundRegisterWorldPropertiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "register_world_properties",
            (buffer, packet) -> {
                buffer.write(NetworkBuffer.VAR_INT, packet.properties().size());
                for (var entry : packet.properties().entrySet()) {
                    PropertyCategory.SERIALIZER.write(buffer, entry.getKey());
                    buffer.write(NetworkBuffer.VAR_INT, entry.getValue().size());
                    for (var property : entry.getValue()) {
                        property.write(packet.player(), buffer);
                    }
                }
            }
    );

    @Override
    public Type<AxiomClientboundRegisterWorldPropertiesPacket> getType() {
        return TYPE;
    }
}

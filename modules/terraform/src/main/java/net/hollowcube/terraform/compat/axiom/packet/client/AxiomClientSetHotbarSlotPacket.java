package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomClientSetHotbarSlotPacket(
        byte index,
        @NotNull ItemStack itemStack
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetHotbarSlotPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.BYTE, AxiomClientSetHotbarSlotPacket::index,
            ItemStack.NETWORK_TYPE, AxiomClientSetHotbarSlotPacket::itemStack,
            AxiomClientSetHotbarSlotPacket::new);
}

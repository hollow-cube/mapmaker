package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetHotbarSlotPacket(
        int index,
        @NotNull ItemStack itemStack
) implements AxiomClientPacket {

    public AxiomClientSetHotbarSlotPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(NetworkBuffer.BYTE), buffer.read(NetworkBuffer.ITEM));
    }

}

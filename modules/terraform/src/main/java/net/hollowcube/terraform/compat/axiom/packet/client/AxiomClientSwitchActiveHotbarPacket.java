package net.hollowcube.terraform.compat.axiom.packet.client;

import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSwitchActiveHotbarPacket(
        int oldHotbarIndex,
        int activeHotbarIndex,
        @NotNull List<@NotNull ItemStack> hotbarItems // Always 9 long
) implements AxiomClientPacket {

    public AxiomClientSwitchActiveHotbarPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.read(NetworkBuffer.BYTE), buffer.read(NetworkBuffer.BYTE), readHotbarItems(buffer, apiVersion));
    }

    private static @NotNull List<ItemStack> readHotbarItems(@NotNull NetworkBuffer buffer, int apiVersion) {
        var items = new ArrayList<ItemStack>(9);
        for (int i = 0; i < 9; i++)
            items.add(buffer.read(ItemStack.NETWORK_TYPE));
        return List.copyOf(items);
    }

}

package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record AxiomClientSwitchActiveHotbarPacket(
        byte oldHotbarIndex,
        byte activeHotbarIndex,
        @NotNull List<@NotNull ItemStack> hotbarItems // Always 9 long
) implements AxiomClientPacket {
    private static final NetworkBuffer.Type<List<ItemStack>> HOTBAR_ITEMS = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, List<ItemStack> value) {
            for (var item : value) {
                buffer.write(ItemStack.NETWORK_TYPE, item);
            }
        }

        @Override
        public List<ItemStack> read(@NotNull NetworkBuffer buffer) {
            var items = new ArrayList<ItemStack>(9);
            for (int i = 0; i < 9; i++)
                items.add(buffer.read(ItemStack.NETWORK_TYPE));
            return List.copyOf(items);
        }
    };
    public static final NetworkBuffer.Type<AxiomClientSwitchActiveHotbarPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.BYTE, AxiomClientSwitchActiveHotbarPacket::oldHotbarIndex,
            NetworkBuffer.BYTE, AxiomClientSwitchActiveHotbarPacket::activeHotbarIndex,
            HOTBAR_ITEMS, AxiomClientSwitchActiveHotbarPacket::hotbarItems,
            AxiomClientSwitchActiveHotbarPacket::new);

    public AxiomClientSwitchActiveHotbarPacket {
        Check.argCondition(oldHotbarIndex < 0 || oldHotbarIndex > 8, "Old hotbar index out of bounds");
        Check.argCondition(activeHotbarIndex < 0 || activeHotbarIndex > 8, "Active hotbar index out of bounds");
        Check.argCondition(hotbarItems.size() != 9, "Hotbar items list must be 9 long");
    }

}

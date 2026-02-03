package net.hollowcube.compat.noxesium.rules;

import net.minestom.server.component.DataComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record NoxesiumServerRule<T>(
        @MagicConstant(valuesFromClass = NoxesiumRuleIds.Server.class) int id,
        NetworkBuffer.Type<T> codec
) {

    public static final NetworkBuffer.Type<NoxesiumServerRule<?>> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(
            NoxesiumServerRules::byId,
            NoxesiumServerRule::id
    );

    public static NoxesiumServerRule<Boolean> Boolean(@MagicConstant(valuesFromClass = NoxesiumRuleIds.Server.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.BOOLEAN);
    }

    public static NoxesiumServerRule<Integer> Integer(@MagicConstant(valuesFromClass = NoxesiumRuleIds.Server.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.VAR_INT);
    }

    public static NoxesiumServerRule<List<ItemStack>> ItemStacks(@MagicConstant(valuesFromClass = NoxesiumRuleIds.Server.class) int id) {
        return new NoxesiumServerRule<>(id, NOXESIUM_ITEM_STACK_NETWORK_TYPE.list());
    }

    private static final NetworkBuffer.Type<ItemStack> NOXESIUM_ITEM_STACK_NETWORK_TYPE = new NetworkBuffer.Type<>() {
        @Override
        public void write(@NotNull NetworkBuffer buffer, @NotNull ItemStack itemStack) {
            if (itemStack.isAir()) {
                buffer.write(NetworkBuffer.VAR_INT, 0);
                return;
            }

            buffer.write(NetworkBuffer.VAR_INT, itemStack.amount());
            buffer.write(NetworkBuffer.STRING, itemStack.material().name());

            final List<DataComponent.Value> added = new ArrayList<>();
            final List<DataComponent.Value> removed = new ArrayList<>();
            for (var entry : itemStack.componentPatch().entrySet()) {
                (entry.value() == null ? removed : added).add(entry);
            }

            buffer.write(NetworkBuffer.VAR_INT, added.size());
            buffer.write(NetworkBuffer.VAR_INT, removed.size());
            for (var addedComponent : added) {
                buffer.write(NetworkBuffer.KEY, addedComponent.component().key());
                ((DataComponent<Object>) addedComponent.component()).write(buffer, addedComponent.value());
            }
            for (var removedComponent : removed) {
                buffer.write(NetworkBuffer.KEY, removedComponent.component().key());
            }
        }

        @Override
        public @NotNull ItemStack read(@NotNull NetworkBuffer buffer) {
            throw new UnsupportedOperationException("noxesium itemstack cannot be decoded from network");
        }
    };
}

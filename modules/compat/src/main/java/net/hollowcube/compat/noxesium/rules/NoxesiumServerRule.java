package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices;
import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.minestom.server.component.DataComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record NoxesiumServerRule<T>(
        @MagicConstant(valuesFromClass = ServerRuleIndices.class) int id,
        NetworkBuffer.Type<T> codec
) {

    public static final NetworkBuffer.Type<NoxesiumServerRule<?>> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(
            NoxesiumServerRules::byId,
            NoxesiumServerRule::id
    );

    public static NoxesiumServerRule<Boolean> Boolean(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.BOOLEAN);
    }

    public static NoxesiumServerRule<Integer> Integer(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.VAR_INT);
    }

    public static NoxesiumServerRule<List<ItemStack>> ItemStacks(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, NOXESIUM_ITEM_STACK_NETWORK_TYPE.list());
    }

    public static NoxesiumServerRule<Map<String, QibDefinition>> QibBehavior(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        NetworkBuffer.Type<QibDefinition> codec = NetworkBuffer.STRING.transform(
                it -> QibDefinition.QIB_GSON.fromJson(it, QibDefinition.class),
                QibDefinition.QIB_GSON::toJson
        );
        return new NoxesiumServerRule<>(id, NetworkBuffer.STRING.mapValue(codec));
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

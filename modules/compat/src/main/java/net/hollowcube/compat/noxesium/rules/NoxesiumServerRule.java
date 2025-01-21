package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;

import java.util.List;

public record NoxesiumServerRule<T>(
        @MagicConstant(valuesFromClass = ServerRuleIndices.class) int id,
        NetworkBuffer.Type<T> codec
) {

    public static NoxesiumServerRule<Boolean> Boolean(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.BOOLEAN);
    }

    public static NoxesiumServerRule<Integer> Integer(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, NetworkBuffer.VAR_INT);
    }

    public static NoxesiumServerRule<List<ItemStack>> ItemStacks(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        return new NoxesiumServerRule<>(id, ItemStack.NETWORK_TYPE.list());
    }
}

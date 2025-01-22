package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices;
import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;

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
        return new NoxesiumServerRule<>(id, ItemStack.NETWORK_TYPE.list());
    }

    public static NoxesiumServerRule<Map<String, QibDefinition>> QibBehavior(@MagicConstant(valuesFromClass = ServerRuleIndices.class) int id) {
        NetworkBuffer.Type<QibDefinition> codec = NetworkBuffer.STRING.transform(
                it -> QibDefinition.QIB_GSON.fromJson(it, QibDefinition.class),
                QibDefinition.QIB_GSON::toJson
        );
        return new NoxesiumServerRule<>(id, NetworkBuffer.STRING.mapValue(codec));
    }
}

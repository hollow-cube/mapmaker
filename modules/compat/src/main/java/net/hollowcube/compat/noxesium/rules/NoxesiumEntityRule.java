package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.EntityRuleIndices;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;

public record NoxesiumEntityRule<T>(
        @MagicConstant(valuesFromClass = EntityRuleIndices.class) int id,
        NetworkBuffer.Type<T> codec
) {

    public static final NetworkBuffer.Type<NoxesiumEntityRule<?>> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(
            NoxesiumEntityRules::byId,
            NoxesiumEntityRule::id
    );

    public static NoxesiumEntityRule<Double> Double(@MagicConstant(valuesFromClass = EntityRuleIndices.class) int id) {
        return new NoxesiumEntityRule<>(id, NetworkBuffer.DOUBLE);
    }

    public static NoxesiumEntityRule<String> String(@MagicConstant(valuesFromClass = EntityRuleIndices.class) int id) {
        return new NoxesiumEntityRule<>(id, NetworkBuffer.STRING);
    }
}

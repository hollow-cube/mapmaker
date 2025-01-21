package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.EntityRuleIndices;
import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import org.intellij.lang.annotations.MagicConstant;

import java.util.List;

public record NoxesiumEntityRule<T>(
        @MagicConstant(valuesFromClass = EntityRuleIndices.class) int id,
        NetworkBuffer.Type<T> codec
) {

    public static NoxesiumEntityRule<Double> Double(@MagicConstant(valuesFromClass = EntityRuleIndices.class) int id) {
        return new NoxesiumEntityRule<>(id, NetworkBuffer.DOUBLE);
    }

    public static NoxesiumEntityRule<String> String(@MagicConstant(valuesFromClass = EntityRuleIndices.class) int id) {
        return new NoxesiumEntityRule<>(id, NetworkBuffer.STRING);
    }
}

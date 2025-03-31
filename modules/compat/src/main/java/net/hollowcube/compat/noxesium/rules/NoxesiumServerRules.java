package net.hollowcube.compat.noxesium.rules;

import com.noxcrew.noxesium.api.protocol.rule.ServerRuleIndices;
import com.noxcrew.noxesium.api.qib.QibDefinition;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;

public final class NoxesiumServerRules {

    private static final Int2ObjectMap<NoxesiumServerRule<?>> RULES = new Int2ObjectArrayMap<>();

    public static final NoxesiumServerRule<Integer> HELD_ITEM_NAME_OFFSET = register(NoxesiumServerRule.Integer(ServerRuleIndices.HELD_ITEM_NAME_OFFSET));
    public static final NoxesiumServerRule<List<ItemStack>> CREATIVE_TAB = register(NoxesiumServerRule.ItemStacks(ServerRuleIndices.CUSTOM_CREATIVE_ITEMS));
    public static final NoxesiumServerRule<Map<String, QibDefinition>> QIBS = register(NoxesiumServerRule.QibBehavior(ServerRuleIndices.QIB_BEHAVIORS));
    public static final NoxesiumServerRule<Boolean> DISABLE_SPIN_ATTACK_COLLISIONS = register(NoxesiumServerRule.Boolean(ServerRuleIndices.DISABLE_SPIN_ATTACK_COLLISIONS));

    @ApiStatus.Internal
    private static <T> NoxesiumServerRule<T> register(NoxesiumServerRule<T> rule) {
        RULES.put(rule.id(), rule);
        return rule;
    }

    @ApiStatus.Internal
    public static NoxesiumServerRule<?> byId(int id) {
        return RULES.get(id);
    }
}

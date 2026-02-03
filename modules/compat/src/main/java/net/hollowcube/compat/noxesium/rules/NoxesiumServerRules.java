package net.hollowcube.compat.noxesium.rules;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public final class NoxesiumServerRules {

    private static final Int2ObjectMap<NoxesiumServerRule<?>> RULES = new Int2ObjectArrayMap<>();

    public static final NoxesiumServerRule<Integer> HELD_ITEM_NAME_OFFSET = register(NoxesiumServerRule.Integer(NoxesiumRuleIds.Server.HELD_ITEM_NAME_OFFSET));
    public static final NoxesiumServerRule<List<ItemStack>> CREATIVE_TAB = register(NoxesiumServerRule.ItemStacks(NoxesiumRuleIds.Server.CUSTOM_CREATIVE_ITEMS));
    public static final NoxesiumServerRule<Boolean> DISABLE_SPIN_ATTACK_COLLISIONS = register(NoxesiumServerRule.Boolean(NoxesiumRuleIds.Server.DISABLE_SPIN_ATTACK_COLLISIONS));
    public static final NoxesiumServerRule<Boolean> CAMERA_LOCKED = register(NoxesiumServerRule.Boolean(NoxesiumRuleIds.Server.CAMERA_LOCKED));

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

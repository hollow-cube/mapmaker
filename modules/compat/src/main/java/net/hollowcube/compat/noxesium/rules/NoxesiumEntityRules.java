package net.hollowcube.compat.noxesium.rules;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.ApiStatus;

public final class NoxesiumEntityRules {

    private static final Int2ObjectMap<NoxesiumEntityRule<?>> RULES = new Int2ObjectArrayMap<>();

    public static final NoxesiumEntityRule<Double> QIB_WIDTH_Z = register(NoxesiumEntityRule.Double(NoxesiumRuleIds.Entity.QIB_WIDTH_Z));
    public static final NoxesiumEntityRule<String> QIB_BEHAVIOR = register(NoxesiumEntityRule.String(NoxesiumRuleIds.Entity.QIB_BEHAVIOR));

    @ApiStatus.Internal
    private static <T> NoxesiumEntityRule<T> register(NoxesiumEntityRule<T> rule) {
        RULES.put(rule.id(), rule);
        return rule;
    }

    @ApiStatus.Internal
    public static NoxesiumEntityRule<?> byId(int id) {
        return RULES.get(id);
    }
}

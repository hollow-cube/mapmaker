package net.hollowcube.mapmaker.dev;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.feature.FeatureFlagProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

@AutoService(FeatureFlagProvider.class)
public class StaticFeatureFlagProvider implements FeatureFlagProvider {
    private final Map<String, BiPredicate<String, List<String>>> flags = new HashMap<>();

    private final Set<String> ADMIN_UUIDS = Set.of(
            "aceb326f-da15-45bc-bf2f-11940c21780c", // notmattw
            "a3634428-40a0-45b3-8583-a3b5813d64c5", // SethPRG
            "ed017f08-fd89-46e2-bba0-495686319801"  // Ontal
    );

    public StaticFeatureFlagProvider() {
        BiPredicate<String, List<String>> staticAdminSet = (name, context) -> {
            if (context.size() != 1) return false;
            return ADMIN_UUIDS.contains(context.get(0));
        };

        flags.put("map.rate_map", staticAdminSet);
    }

    @Override
    public boolean test(@NotNull String name, @NotNull String... context) {
        var flag = flags.get(name);
        return flag != null && flag.test(name, List.of(context));
    }
}

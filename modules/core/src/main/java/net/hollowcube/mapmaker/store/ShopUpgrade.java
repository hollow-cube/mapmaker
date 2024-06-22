package net.hollowcube.mapmaker.store;

import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.perm.PlatformPermLike;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public enum ShopUpgrade {
    BUILD_TOOLS("build_tools", new CostList(CostEntry.Cubits.INSTANCE, 50)),

    MAP_SLOT_3("map_slot_3", new CostList(CostEntry.Cubits.INSTANCE, 50)),
    MAP_SLOT_4("map_slot_4", new CostList(CostEntry.Cubits.INSTANCE, 100)),
    MAP_SLOT_5("map_slot_5", new CostList(CostEntry.Cubits.INSTANCE, 150)),

    MAP_SIZE_2("map_size_2", new CostList(CostEntry.Cubits.INSTANCE, 50)),
    MAP_SIZE_3("map_size_3", new CostList(CostEntry.Cubits.INSTANCE, 100)),
    MAP_SIZE_4("map_size_4", new CostList(CostEntry.Cubits.INSTANCE, 150)),
    ;

    private final String id;
    private final CostList cost;

    private final PlatformPermLike directPerm;
    private final PlatformPermLike indirectPerm;

    ShopUpgrade(@NotNull String id, @NotNull CostList cost) {
        this.id = id;
        this.cost = cost;

        var name = name().toLowerCase(Locale.ROOT);
        this.directPerm = PlatformPermLike.of("u_" + name);
        this.indirectPerm = PlatformPermLike.of("upg_" + name);
    }

    public @NotNull PlatformPermLike directPerm() {
        return directPerm;
    }

    public @NotNull PlatformPermLike indirectPerm() {
        return indirectPerm;
    }

    public boolean canAfford(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
        return cost.canAfford(playerData, backpack);
    }

    public void appendLore(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack, @NotNull List<Component> lore) {
        cost.appendLore(playerData, backpack, lore);
    }

    public int cubits() {
        var cubits = cost.entries().get(CostEntry.Cubits.INSTANCE);
        if (cubits == null || cubits < 1)
            throw new IllegalStateException("No cubits cost for " + this);
        return cubits;
    }
}

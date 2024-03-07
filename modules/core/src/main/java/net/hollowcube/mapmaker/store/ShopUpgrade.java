package net.hollowcube.mapmaker.store;

import org.jetbrains.annotations.NotNull;

public enum ShopUpgrade {
    MAP_SIZE_2("map_size_2", new CostList(CostEntry.Cubits.INSTANCE, 50)),
    MAP_SIZE_3("map_size_3", new CostList(CostEntry.Cubits.INSTANCE, 100)),
    MAP_SIZE_4("map_size_4", new CostList(CostEntry.Cubits.INSTANCE, 150)),

    MAP_SLOT_3("map_slot_3", new CostList(CostEntry.Cubits.INSTANCE, 50)),
    MAP_SLOT_4("map_slot_4", new CostList(CostEntry.Cubits.INSTANCE, 100)),
    MAP_SLOT_5("map_slot_5", new CostList(CostEntry.Cubits.INSTANCE, 150)),
    ;

    private final String id;
    private final CostList cost;

    ShopUpgrade(@NotNull String id, @NotNull CostList cost) {
        this.id = id;
        this.cost = cost;
    }
}

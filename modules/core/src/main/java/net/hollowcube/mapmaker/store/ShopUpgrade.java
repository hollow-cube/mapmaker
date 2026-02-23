package net.hollowcube.mapmaker.store;

import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ShopUpgrade {
    BUILD_TOOLS("build_tools", new CostList(CostEntry.Cubits.INSTANCE, 50), 0, MapSize.NORMAL),

    MAP_SLOT_3("map_slot_3", new CostList(CostEntry.Cubits.INSTANCE, 50), 1, MapSize.NORMAL),
    MAP_SLOT_4("map_slot_4", new CostList(CostEntry.Cubits.INSTANCE, 100), 2, MapSize.NORMAL),
    MAP_SLOT_5("map_slot_5", new CostList(CostEntry.Cubits.INSTANCE, 150), 3, MapSize.NORMAL),

    MAP_SIZE_2("map_size_2", new CostList(CostEntry.Cubits.INSTANCE, 50), 0, MapSize.LARGE),
    MAP_SIZE_3("map_size_3", new CostList(CostEntry.Cubits.INSTANCE, 100), 0, MapSize.MASSIVE),
    MAP_SIZE_4("map_size_4", new CostList(CostEntry.Cubits.INSTANCE, 150), 0, MapSize.COLOSSAL),
    ;

    public static final Map<String, ShopUpgrade> BY_ID = Arrays.stream(ShopUpgrade.values())
        .collect(Collectors.toMap(value -> value.id, Function.identity()));

    private final String id;
    private final CostList cost;
    private final int mapSlots;
    private final MapSize maxMapSize;

    ShopUpgrade(@NotNull String id, @NotNull CostList cost, int mapSlots, MapSize maxMapSize) {
        this.id = id;
        this.cost = cost;
        this.mapSlots = mapSlots;
        this.maxMapSize = maxMapSize;
    }

    public boolean canAfford(@NotNull PlayerData playerData, @NotNull PlayerBackpack backpack) {
        return cost.canAfford(playerData, backpack);
    }

    public void appendLore(@NotNull PlayerData playerData, @NotNull PlayerBackpack backpack, @NotNull List<Component> lore) {
        cost.appendLore(playerData, backpack, lore);
    }

    public int cubits() {
        var cubits = cost.entries().get(CostEntry.Cubits.INSTANCE);
        if (cubits == null || cubits < 1)
            throw new IllegalStateException("No cubits cost for " + this);
        return cubits;
    }

    public int mapSlots() {
        return mapSlots;
    }

    public MapSize maxMapSize() {
        return maxMapSize;
    }

    public boolean has(PlayerData playerData) {
        if ((this == MAP_SIZE_2 || this == MAP_SIZE_3) && playerData.has(Permission.EXTENDED_LIMITS))
            return true;
        return switch (this) {
            case BUILD_TOOLS -> false;
            case MAP_SLOT_3, MAP_SLOT_4, MAP_SLOT_5 -> {
                int upgradeSlots = playerData.mapSlots() - 2;
                if (playerData.isHypercube()) upgradeSlots -= 3;
                yield upgradeSlots >= mapSlots;
            }
            case MAP_SIZE_2, MAP_SIZE_3, MAP_SIZE_4 -> maxMapSize().id() <= playerData.maxMapSize().id();
        };
    }
}

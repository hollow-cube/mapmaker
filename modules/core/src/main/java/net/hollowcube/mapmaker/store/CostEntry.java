package net.hollowcube.mapmaker.store;


import com.mojang.serialization.Codec;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface CostEntry permits CostEntry.Cubits, CostEntry.Coins, CostEntry.BackpackItem {
    Codec<CostEntry> CODEC = Codec.STRING.xmap(ti -> {
        if (ti.equals("cubits")) return CostEntry.Cubits.INSTANCE;
        if (ti.equals("coins")) return CostEntry.Coins.INSTANCE;
        return new CostEntry.BackpackItem(net.hollowcube.mapmaker.backpack.BackpackItem.byId(ti));
    }, ti -> {
        if (ti instanceof CostEntry.Cubits) return "cubits";
        if (ti instanceof CostEntry.Coins) return "coins";
        return ((CostEntry.BackpackItem) ti).entry.id();
    });

    @NotNull
    Component displayName();
    int getCount(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack);

    final class Cubits implements CostEntry {
        public static final Cubits INSTANCE = new Cubits();

        @Override
        public @NotNull Component displayName() {
            return Component.translatable("icon.cubits");
        }

        @Override
        public int getCount(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
            return playerData.cubits();
        }
    }

    final class Coins implements CostEntry {
        public static final Coins INSTANCE = new Coins();

        @Override
        public @NotNull Component displayName() {
            return Component.translatable("icon.coins");
        }

        @Override
        public int getCount(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
            return playerData.coins();
        }
    }

    record BackpackItem(@Nullable net.hollowcube.mapmaker.backpack.BackpackItem entry) implements CostEntry {
        private static final Component NULL_COMPONENT = Component.text("null");

        @Override
        public @NotNull Component displayName() {
            return entry == null ? NULL_COMPONENT : entry.iconComponent().append(Component.text(FontUtil.computeOffset(2)));
        }

        @Override
        public int getCount(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
            if (entry == null) return 0;
            return backpack.getQuantity(entry);
        }
    }
}

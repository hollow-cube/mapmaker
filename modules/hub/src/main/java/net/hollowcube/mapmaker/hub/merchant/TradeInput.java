package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed interface TradeInput permits TradeInput.Cubits, TradeInput.Coins, TradeInput.BackpackItem {
    Codec<TradeInput> CODEC = Codec.STRING.xmap(ti -> {
        if (ti.equals("cubits")) return TradeInput.Cubits.INSTANCE;
        if (ti.equals("coins")) return TradeInput.Coins.INSTANCE;
        return new TradeInput.BackpackItem(net.hollowcube.mapmaker.backpack.BackpackItem.byId(ti));
    }, ti -> {
        if (ti instanceof TradeInput.Cubits) return "cubits";
        if (ti instanceof TradeInput.Coins) return "coins";
        return ((TradeInput.BackpackItem) ti).entry.id();
    });

    @NotNull
    Component displayName();
    int getCount(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack);

    final class Cubits implements TradeInput {
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

    final class Coins implements TradeInput {
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

    record BackpackItem(@Nullable net.hollowcube.mapmaker.backpack.BackpackItem entry) implements TradeInput {
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

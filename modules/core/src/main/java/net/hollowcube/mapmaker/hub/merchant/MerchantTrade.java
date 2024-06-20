package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.store.CostList;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//TODO: Move me back to the hub module when no longer used in cosmetic view
public record MerchantTrade(
        @NotNull Cosmetic result,
        @NotNull CostList inputs
) {
    public static final Codec<MerchantTrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Cosmetic.CODEC.fieldOf("result").forGetter(MerchantTrade::result),
            CostList.CODEC.fieldOf("inputs").forGetter(MerchantTrade::inputs)
    ).apply(i, MerchantTrade::new));

    public void appendLore(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack, @NotNull List<Component> lore) {
        var canAfford = inputs.appendLore(playerData, backpack, lore);

        lore.add(Component.empty());
        lore.add(Component.translatable(canAfford ? "merchant.trade.buy" : "merchant.trade.cannot_afford"));
    }

    public boolean canAfford(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
        return inputs.canAfford(playerData, backpack);
    }

}

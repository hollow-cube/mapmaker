package net.hollowcube.mapmaker.hub.merchant;

import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.store.CostList;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.StructCodec;

import java.util.List;

//TODO: Move me back to the hub module when no longer used in cosmetic view
public record MerchantTrade(
    Cosmetic result,
    CostList inputs
) {
    public static final StructCodec<MerchantTrade> CODEC = StructCodec.struct(
            "result", Cosmetic.CODEC, MerchantTrade::result,
            "inputs", CostList.CODEC, MerchantTrade::inputs,
            MerchantTrade::new);

    public void appendLore(PlayerData playerData, PlayerBackpack backpack, List<Component> lore) {
        var canAfford = inputs.appendLore(playerData, backpack, lore);

        lore.add(Component.empty());
        lore.add(Component.translatable(canAfford ? "merchant.trade.buy" : "merchant.trade.cannot_afford"));
    }

    public boolean canAfford(PlayerData playerData, PlayerBackpack backpack) {
        return inputs.canAfford(playerData, backpack);
    }

}

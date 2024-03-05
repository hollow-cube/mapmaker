package net.hollowcube.mapmaker.hub.merchant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record MerchantTrade(
        @NotNull Cosmetic result,
        @NotNull Map<TradeInput, Integer> inputs
) {
    public static final Codec<MerchantTrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Cosmetic.CODEC.fieldOf("result").forGetter(MerchantTrade::result),
            Codec.unboundedMap(TradeInput.CODEC, Codec.INT).fieldOf("inputs").forGetter(MerchantTrade::inputs)
    ).apply(i, MerchantTrade::new));

    private static final TextColor RED = TextColor.fromCSSHexString("#fa4141");
    private static final TextColor GREEN = TextColor.fromCSSHexString("#46FA32");
    private static final TextColor GRAY = TextColor.fromCSSHexString("#B0B0B0");

    public void appendLore(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack, @NotNull List<Component> lore) {
        boolean canAfford = true, canP2W = false; //todo add support for buying with cubits later
        for (var entry : inputs.entrySet()) {
            var type = entry.getKey();
            var amount = entry.getValue();
            var owned = type.getCount(playerData, backpack);
            canAfford &= owned >= amount;

            var component = LanguageProviderV2.BASE_EMPTY
                    .append(type.displayName())
                    .append(Component.text(owned, owned >= amount ? GREEN : RED))
                    .append(Component.text("/" + amount + " ", GRAY));
            lore.add(component);
        }

        lore.add(Component.empty());
        lore.add(Component.translatable(canAfford ? "merchant.trade.buy" : "merchant.trade.cannot_afford"));
    }

    public boolean canAfford(@NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack) {
        for (var entry : inputs.entrySet()) {
            if (entry.getKey().getCount(playerData, backpack) < entry.getValue())
                return false;
        }
        return true;
    }

}

package net.hollowcube.mapmaker.store;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.codec.Codec;

import java.util.List;
import java.util.Map;

public record CostList(
    Map<CostEntry, Integer> entries
) {
    public static final Codec<CostList> CODEC = CostEntry.CODEC.mapValue(Codec.INT).transform(CostList::new, CostList::entries);

    private static final TextColor RED = TextColor.fromCSSHexString("#fa4141");
    private static final TextColor GREEN = TextColor.fromCSSHexString("#46FA32");
    private static final TextColor GRAY = TextColor.fromCSSHexString("#B0B0B0");

    public CostList(CostEntry entry, int amount) {
        this(Map.of(entry, amount));
    }

    public boolean appendLore(PlayerData playerData, PlayerBackpack backpack, List<Component> lore) {
        boolean canAfford = true, canP2W = false; //todo add support for buying with cubits later
        for (var entry : entries.entrySet()) {
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

        return canAfford;
    }

    public boolean canAfford(PlayerData playerData, PlayerBackpack backpack) {
        for (var entry : entries.entrySet()) {
            if (entry.getKey().getCount(playerData, backpack) < entry.getValue())
                return false;
        }
        return true;
    }
}

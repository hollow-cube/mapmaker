package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.store.CostEntry;
import net.hollowcube.mapmaker.store.CostList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CosmeticPrices {

    // This is pretty giga yikes and temporary. Just exists while cosmetics are being sold for cubits.
    private static final Map<String, MerchantTrade> TEMP_COSMETIC_TRADES;

    static {
        var costs = new HashMap<String, Integer>();

        // Hats
        costs.put("hat/top_hat", 8);
        costs.put("hat/sunglasses", 5);
        costs.put("hat/hard_hat", 12);
        costs.put("hat/crown", 10);
        costs.put("hat/straw_hat", 10);
        costs.put("hat/clown_mask", 28);
        costs.put("hat/bikers_helmet", 20);
        costs.put("hat/samurai_helmet", 25);
        costs.put("hat/kitsune_mask", 32);
        costs.put("hat/apprentice_hat", 25);
        costs.put("hat/wizard_hat", 30);
        costs.put("hat/knight_helmet", 35);
        costs.put("hat/evil_clown_mask", 40);
        costs.put("hat/oni_mask", 40);
        costs.put("hat/shark_hat", 40);

        // Accessories
        costs.put("accessory/donut", 8);
        costs.put("accessory/wrench", 15);
        costs.put("accessory/training_sword", 12);
        costs.put("accessory/shonk", 25);
        costs.put("accessory/burger", 25);
        costs.put("accessory/dynamite", 18);
        costs.put("accessory/knights_sword", 22);
        costs.put("accessory/cyberfist", 35);
        costs.put("accessory/coffee_cup", 32);
        costs.put("accessory/drill", 45);
        costs.put("accessory/excalibur", 50);
        costs.put("accessory/shrinking_device", 40);

        // Particles
        //        costs.put("particle/cloud", 5);
        //        costs.put("particle/bubble", 5);
        //        costs.put("particle/note", 5);
        costs.put("particle/cherry_leaves", 5);

        // Victory Effects
        //        costs.put("victory_effect/explosion", 5); // Intentionally not present, it does not work so should not be buyable
        //        costs.put("victory_effect/lightning", 5); // Intentionally not present, it does not work so should not be buyable
        costs.put("victory_effect/firework", 3);
        //        costs.put("victory_effect/omega", 15); // Intentionally not present, it does not work so should not be buyable

        var tempTrades = new HashMap<String, MerchantTrade>();
        for (var entry : costs.entrySet()) {
            tempTrades.put(entry.getKey(), new MerchantTrade(
                Cosmetic.byPathRequired(entry.getKey()),
                new CostList(Map.of(CostEntry.Cubits.INSTANCE, entry.getValue()))
            ));
        }
        TEMP_COSMETIC_TRADES = Map.copyOf(tempTrades);
    }

    public static @Nullable MerchantTrade getTrade(Cosmetic cosmetic) {
        return TEMP_COSMETIC_TRADES.get(cosmetic.path());
    }
}

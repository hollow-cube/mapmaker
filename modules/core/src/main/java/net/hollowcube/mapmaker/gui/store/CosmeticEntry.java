package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.CostEntry;
import net.hollowcube.mapmaker.store.CostList;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosmeticEntry extends View {
    public static final String UPDATE_SELECTED = "cosmetic_entry.update_selected";
    private static final BadSprite[] SELECTED_SPRITES = new BadSprite[]{
            BadSprite.require("cosmetic/selector/selected_row_1"),
            BadSprite.require("cosmetic/selector/selected_row_2"),
            BadSprite.require("cosmetic/selector/selected_row_3"),
            BadSprite.require("cosmetic/selector/selected_row_4"),
            BadSprite.require("cosmetic/selector/selected_row_5"),
    };

    // This is pretty giga yikes and temporary. Just exists while cosmetics are being sold for cubits.
    private static final Map<String, MerchantTrade> TEMP_COSMETIC_TRADES;

    static {
        var costs = new HashMap<String, Integer>();

        // Hats
        costs.put("hat/top_hat", 999);
        costs.put("hat/sunglasses", 999);
        costs.put("hat/hard_hat", 999);
        costs.put("hat/crown", 999);
        costs.put("hat/clown_mask", 999);
        costs.put("hat/bikers_helmet", 999);
        costs.put("hat/samurai_helmet", 999);
        costs.put("hat/kitsune_mask", 999);
        costs.put("hat/apprentice_hat", 999);
        costs.put("hat/wizard_hat", 999);
        costs.put("hat/knight_helmet", 999);
        costs.put("hat/evil_clown_mask", 999);
        costs.put("hat/oni_mask", 999);
        costs.put("hat/shark_hat", 999);

        // Accessories
        costs.put("accessory/donut", 999);
        costs.put("accessory/wrench", 999);
        costs.put("accessory/training_sword", 999);
        costs.put("accessory/burger", 999);
        costs.put("accessory/dynamite", 999);
        costs.put("accessory/knights_sword", 999);
        costs.put("accessory/cyberfist", 999);
        costs.put("accessory/coffee_cup", 999);
        costs.put("accessory/drill", 999);
        costs.put("accessory/excalibur", 999);
        costs.put("accessory/shrinking_device", 999);

        // Particles
        costs.put("particle/cloud", 999);
        costs.put("particle/bubble", 999);
        costs.put("particle/note", 999);

        // Victory Effects
//        costs.put("victory_effect/explosion", 999); // Intentionally not present, it does not work so should not be buyable
//        costs.put("victory_effect/lightning", 999); // Intentionally not present, it does not work so should not be buyable
        costs.put("victory_effect/firework", 999);
//        costs.put("victory_effect/omega", 999); // Intentionally not present, it does not work so should not be buyable

        var tempTrades = new HashMap<String, MerchantTrade>();
        for (var entry : costs.entrySet()) {
            tempTrades.put(entry.getKey(), new MerchantTrade(
                    Cosmetic.byPathRequired(entry.getKey()),
                    new CostList(Map.of(CostEntry.Cubits.INSTANCE, entry.getValue()))
            ));
        }
        TEMP_COSMETIC_TRADES = Map.copyOf(tempTrades);
    }

    private @ContextObject Player player;
    private @ContextObject PlayerService playerService;

    private @Outlet("root") Switch rootSwitch;
    private @Outlet("off") Label offIcon;
    private @Outlet("on") Label onIcon;

    private final PlayerDataV2 playerData;
    private final Cosmetic cosmetic;
    private final boolean isLocked;
    private final int row;

    public CosmeticEntry(@NotNull Context context, @NotNull PlayerDataV2 playerData, @NotNull PlayerBackpack backpack, @NotNull Cosmetic cosmetic, boolean isLocked, int row) {
        super(context);
        this.playerData = playerData;
        this.cosmetic = cosmetic;
        this.isLocked = isLocked;
        this.row = row;

        var itemIcon = isLocked ? cosmetic.iconLockedItem() : cosmetic.iconItem();
        offIcon.setItemSprite(itemIcon);
        onIcon.setItemSprite(itemIcon);

        var sprite = SELECTED_SPRITES[row];
        onIcon.setSprite(sprite.fontChar(), sprite.cmd(), sprite.width(), sprite.offsetX(), sprite.rightOffset());

        {
            var lore = new ArrayList<>(itemIcon.get(ItemComponent.LORE, List.of()));
            lore.add(Component.text(""));
            lore.add(Component.translatable("cosmetic.deselect"));
            onIcon.setComponentsDirect(itemIcon.get(ItemComponent.CUSTOM_NAME), lore);
        }

        {
            var lore = new ArrayList<>(itemIcon.get(ItemComponent.LORE, List.of()));
            var trade = TEMP_COSMETIC_TRADES.get(cosmetic.path());
            lore.add(Component.empty());
            if (isLocked && trade != null) {
                trade.appendLore(playerData, backpack, lore);
            } else {
                lore.add(Component.translatable(isLocked ? "cosmetic.locked" : "cosmetic.select"));
            }
            offIcon.setComponentsDirect(itemIcon.get(ItemComponent.CUSTOM_NAME), lore);
        }

        rootSwitch.setOption(isSelected() ? 1 : 0);
    }

    @Action("off")
    public void handleSelectCosmetic(@NotNull Player player) {
        if (isLocked) {
            tryBuyCosmetic();
            return;
        }

        playerData.setCosmetic(cosmetic.type(), cosmetic);
        performSignal(UPDATE_SELECTED);
    }

    @Action("on")
    public void handleDeselectCosmetic(@NotNull Player player) {
        playerData.setCosmetic(cosmetic.type(), null);
        performSignal(UPDATE_SELECTED);
    }

    @Signal(UPDATE_SELECTED)
    public void handleSelectionChange() {
        if (isSelected() != (rootSwitch.getOption() == 1)) {
            rootSwitch.setOption(isSelected() ? 1 : 0);
        }
    }

    private boolean isSelected() {
        return cosmetic.id().equals(playerData.getCosmetic(cosmetic.type()));
    }

    private void tryBuyCosmetic() {
        // DELETE ME WHEN NO LONGER SELLING COSMETICS HERE DIRECTLY. THIS LOGIC IS DUPLICATED FROM TradeEntry.
        var trade = TEMP_COSMETIC_TRADES.get(cosmetic.path());
        if (trade == null || !trade.canAfford(PlayerDataV2.fromPlayer(player), PlayerBackpack.fromPlayer(player)))
            return;

        Integer cubits = null;
        for (var entry : trade.inputs().entries().entrySet()) {
            var type = entry.getKey();
            if (type instanceof CostEntry.Cubits)
                cubits = entry.getValue();
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        playerService.buyCosmetic(playerData.id(), trade.result(), null, cubits, new JsonObject());

        var icon = trade.result().iconItem();
        player.sendMessage(Component.translatable("merchant.trade.success", icon.get(ItemComponent.CUSTOM_NAME, Component.empty()).hoverEvent(icon.asHoverEvent())));
        player.closeInventory();
    }
}

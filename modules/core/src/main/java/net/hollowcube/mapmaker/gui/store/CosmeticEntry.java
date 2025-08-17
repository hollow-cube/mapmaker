package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.canvas.ClickType;
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
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.gui.common.ConfirmAction;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.CostEntry;
import net.hollowcube.mapmaker.store.CostList;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
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

    private @ContextObject Player player;
    private @ContextObject PlayerService playerService;

    private @Outlet("root") Switch rootSwitch;
    private @Outlet("off") Label offIcon;
    private @Outlet("on") Label onIcon;

    private final PlayerDataV2 playerData;
    private final Cosmetic cosmetic;
    private final boolean isLocked;
    private final int row;

    private ItemStack offItemStack = null;
    private boolean isPreviewing = false;

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
        onIcon.setSprite(sprite.fontChar(), sprite.modelOrNull(), sprite.width(), sprite.offsetX(), sprite.rightOffset());

        {
            var lore = new ArrayList<>(itemIcon.get(DataComponents.LORE, List.of()));
            lore.add(Component.text(""));
            lore.add(Component.translatable("cosmetic.deselect"));
            onIcon.setComponentsDirect(itemIcon.get(DataComponents.CUSTOM_NAME), lore);
        }

        {
            var lore = new ArrayList<>(itemIcon.get(DataComponents.LORE, List.of()));
            var trade = TEMP_COSMETIC_TRADES.get(cosmetic.path());
            lore.add(Component.empty());
            if (isLocked && trade != null) {
                trade.appendLore(playerData, backpack, lore);
            } else {
                lore.add(Component.translatable(isLocked ? "cosmetic.locked" : "cosmetic.select"));
            }
            if (isLocked && cosmetic.impl() instanceof ModelCosmeticImpl) {
                lore.add(Component.empty());
                lore.add(Component.translatable("cosmetic.preview"));
            }
            offItemStack = itemIcon.withLore(lore);
            offIcon.setItemDirect(offItemStack);

        }

        rootSwitch.setOption(isSelected() ? 1 : 0);
    }

    @Action("off")
    public void handleSelectCosmetic(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case LEFT_CLICK -> {
                if (isLocked) {
                    if (!canAfford()) return;
                    String name = PlainTextComponentSerializer.plainText().serialize(cosmetic.displayName());
                    pushView(context -> new ConfirmAction(
                            context,
                            this::tryBuyCosmetic,
                            Component.translatable("purchase the " + name + " cosmetic")
                    ));
                    return;
                }

                playerData.setCosmetic(cosmetic.type(), cosmetic);
                performSignal(UPDATE_SELECTED);
            }
            case RIGHT_CLICK -> {
                // Preview the 3d cosmetic
                if (isPreviewing) {
                    isPreviewing = false;
                    offIcon.setItemDirect(offItemStack);
                } else if (isLocked && cosmetic.impl() instanceof ModelCosmeticImpl model) {
                    isPreviewing = true;
                    var previewItem = model.iconItem().builder()
                            .customName(offItemStack.get(DataComponents.CUSTOM_NAME))
                            .lore(offItemStack.get(DataComponents.LORE))
                            .customModelData(List.of(2f), List.of(), List.of(), List.of())
                            .hideExtraTooltip()
                            .build();
                    offIcon.setItemDirect(previewItem);
                }
            }
        }
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

    private boolean canAfford() {
        var trade = TEMP_COSMETIC_TRADES.get(cosmetic.path());
        return trade != null && trade.canAfford(playerData, PlayerBackpack.fromPlayer(player));
    }

    private void tryBuyCosmetic() {
        // DELETE ME WHEN NO LONGER SELLING COSMETICS HERE DIRECTLY. THIS LOGIC IS DUPLICATED FROM TradeEntry.
        var trade = TEMP_COSMETIC_TRADES.get(cosmetic.path());
        if (!canAfford()) return;

        Integer cubits = null;
        for (var entry : trade.inputs().entries().entrySet()) {
            var type = entry.getKey();
            if (type instanceof CostEntry.Cubits)
                cubits = entry.getValue();
        }

        var playerData = PlayerDataV2.fromPlayer(player);
        playerService.buyCosmetic(playerData.id(), trade.result(), null, cubits, new JsonObject());

        var icon = trade.result().iconItem();
        player.sendMessage(Component.translatable("merchant.trade.success", icon.get(DataComponents.CUSTOM_NAME, Component.empty()).hoverEvent(icon.asHoverEvent())));
        player.closeInventory();
    }
}

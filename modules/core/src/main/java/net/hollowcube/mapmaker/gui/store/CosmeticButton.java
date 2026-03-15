package net.hollowcube.mapmaker.gui.store;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.backpack.PlayerBackpack;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.hub.merchant.MerchantTrade;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.store.CostEntry;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CosmeticButton extends Button {

    private final PlayerService players;

    private final Cosmetic cosmetic;
    private final boolean locked;
    private final PlayerData data;
    private final PlayerBackpack backpack;
    private final @Nullable MerchantTrade cost;
    private final List<CosmeticButton> siblings;

    private boolean previewing = false;

    public CosmeticButton(PlayerService players, Cosmetic cosmetic, boolean locked, PlayerData data,
                          PlayerBackpack backpack, List<CosmeticButton> siblings) {
        super(1, 1);
        this.players = players;
        this.cosmetic = cosmetic;
        this.locked = locked;
        this.data = data;
        this.backpack = backpack;
        this.cost = locked ? CosmeticPrices.getTrade(cosmetic) : null;
        this.siblings = siblings;

        this.update();
        this.onLeftClick(() -> {
            if (this.cost != null && this.cost.canAfford(this.data, this.backpack)) {
                this.host.pushView(ExtraPanels.confirm("Purchase cosmetic?", this::purchase));
            } else if (!this.locked) {
                this.data.setCosmetic(this.cosmetic.type(), this.isSelected() ? null : this.cosmetic);
                this.siblings.forEach(CosmeticButton::update);
            }
        });
        this.onRightClick(() -> {
            this.previewing = !this.previewing;
            this.update();
        });
    }

    private boolean isSelected() {
        return this.cosmetic.id().equals(this.data.getCosmetic(this.cosmetic.type()));
    }

    private void update() {
        var selected = this.isSelected();
        var lore = new ArrayList<Component>();
        lore.add(Component.empty());

        if (selected) {
            lore.add(Component.translatable("cosmetic.deselect"));
        } else if (this.cost != null) {
            this.cost.appendLore(this.data, this.backpack, lore);
        } else {
            lore.add(Component.translatable(this.locked ? "cosmetic.locked" : "cosmetic.select"));
        }
        if (this.cosmetic.canBePreviewed()) lore.add(Component.translatable("cosmetic.preview"));

        this.lorePostfix(lore);
        this.background(selected ? "generic2/slot/selected" : null);

        if (this.previewing && cosmetic.impl() instanceof ModelCosmeticImpl) {
            this.from(this.cosmetic.iconPreviewItem());
        } else {
            this.from(this.locked ? this.cosmetic.iconLockedItem() : this.cosmetic.iconItem());
        }
    }

    private void purchase(Player player) {
        // DELETE ME WHEN NO LONGER SELLING COSMETICS HERE DIRECTLY. THIS LOGIC IS DUPLICATED FROM TradeEntry.
        if (this.cost == null || !this.cost.canAfford(this.data, this.backpack)) return;

        Integer cubits = null;
        for (var entry : this.cost.inputs().entries().entrySet()) {
            var type = entry.getKey();
            if (type instanceof CostEntry.Cubits)
                cubits = entry.getValue();
        }

        this.players.buyCosmetic(this.data.id(), this.cost.result(), null, cubits, new JsonObject());

        var icon = this.cost.result().iconItem();
        player.sendMessage(Component.translatable("merchant.trade.success", icon.get(DataComponents.CUSTOM_NAME, Component.empty()).hoverEvent(icon.asHoverEvent())));
        player.closeInventory();
    }
}

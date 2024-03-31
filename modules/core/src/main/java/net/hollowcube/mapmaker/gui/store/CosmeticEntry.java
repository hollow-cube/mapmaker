package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CosmeticEntry extends View {
    public static final String UPDATE_SELECTED = "cosmetic_entry.update_selected";
    private static final BadSprite[] SELECTED_SPRITES = new BadSprite[]{
            BadSprite.require("cosmetic/selector/selected_row_1"),
            BadSprite.require("cosmetic/selector/selected_row_2"),
            BadSprite.require("cosmetic/selector/selected_row_3"),
            BadSprite.require("cosmetic/selector/selected_row_4"),
            BadSprite.require("cosmetic/selector/selected_row_5"),
    };

    private @Outlet("root") Switch rootSwitch;
    private @Outlet("off") Label offIcon;
    private @Outlet("on") Label onIcon;

    private final PlayerDataV2 playerData;
    private final Cosmetic cosmetic;
    private final boolean isLocked;
    private final int row;

    public CosmeticEntry(@NotNull Context context, @NotNull PlayerDataV2 playerData, @NotNull Cosmetic cosmetic, boolean isLocked, int row) {
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
            var lore = new ArrayList<>(itemIcon.getLore());
            lore.add(Component.text(""));
            lore.add(Component.translatable("cosmetic.deselect"));
            onIcon.setComponentsDirect(itemIcon.getDisplayName(), lore);
        }

        {
            var lore = new ArrayList<>(itemIcon.getLore());
            lore.add(Component.empty());
            lore.add(Component.translatable(isLocked ? "cosmetic.locked" : "cosmetic.select"));
            offIcon.setComponentsDirect(itemIcon.getDisplayName(), lore);
        }

        rootSwitch.setOption(isSelected() ? 1 : 0);
    }

    @Action("off")
    public void handleSelectCosmetic(@NotNull Player player) {
//        if (isLocked) return;

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
}

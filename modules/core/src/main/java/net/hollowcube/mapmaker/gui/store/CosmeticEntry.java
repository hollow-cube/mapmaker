package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.cosmetic.CosmeticType;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CosmeticEntry extends View {
    public static final String UPDATE_SELECTED = "cosmetic_entry.update_selected";

    private @Outlet("root") Switch rootSwitch;
    private @Outlet("off") Label offIcon;
    private @Outlet("on") Label onIcon;

    private final PlayerDataV2 playerData;
    private final Cosmetic cosmetic;

    public CosmeticEntry(@NotNull Context context, @NotNull PlayerDataV2 playerData, @NotNull Cosmetic cosmetic) {
        super(context);
        this.playerData = playerData;
        this.cosmetic = cosmetic;

        var itemIcon = cosmetic.icon();
        offIcon.setItemSprite(itemIcon);
        onIcon.setItemSprite(itemIcon);

        {
            var lore = new ArrayList<>(itemIcon.getLore());
            lore.add(Component.text(""));
            lore.add(Component.translatable("cosmetic.deselect"));
            onIcon.setComponentsDirect(itemIcon.getDisplayName(), lore);
        }

        {
            var lore = new ArrayList<>(itemIcon.getLore());
            lore.add(Component.text(""));
            if (playerData.unlockedCosmetics().contains(cosmetic.id())) {
                lore.add(Component.translatable("cosmetic.select"));
            } else {
                lore.add(Component.translatable("cosmetic.locked"));
            }
            offIcon.setComponentsDirect(itemIcon.getDisplayName(), lore);
        }

        rootSwitch.setOption(isSelected() ? 1 : 0);
    }

    @Action("off")
    public void handleSelectCosmetic(@NotNull Player player) {
//        if (!playerData.unlockedCosmetics().contains(cosmetic.id())) {
//            return;
//        }

        playerData.setCosmetic(CosmeticType.HEAD, cosmetic);
        performSignal(UPDATE_SELECTED);
    }

    @Action("on")
    public void handleDeselectCosmetic(@NotNull Player player) {
        playerData.setCosmetic(CosmeticType.HEAD, null);
        performSignal(UPDATE_SELECTED);
    }

    @Signal(UPDATE_SELECTED)
    public void handleSelectionChange() {
        if (isSelected() != (rootSwitch.getOption() == 1)) {
            rootSwitch.setOption(isSelected() ? 1 : 0);
        }
    }

    private boolean isSelected() {
        return cosmetic.id().equals(playerData.getCosmetic(CosmeticType.HEAD));
    }
}

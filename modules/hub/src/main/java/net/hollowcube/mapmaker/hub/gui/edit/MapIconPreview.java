package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.util.TagUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapIconPreview extends View {
    public static final String SIG_SELECTED = "map_icon_preview.selected";

    private @Outlet("label") Label label;

    private final Material material;
    private final boolean isNoResultsButton;

    public MapIconPreview(@NotNull Context context, @NotNull Material material) {
        super(context);
        this.material = material;
        this.isNoResultsButton = false;

        var iconItem = ItemStack.builder(material);
        TagUtil.removeTooltipExtras(iconItem);
        iconItem.set(ItemComponent.ENCHANTMENT_GLINT_OVERRIDE, false);
        label.setItemSprite(iconItem.build());
        label.setArgs(LanguageProviderV2.getVanillaTranslation(material));
    }

    public MapIconPreview(@NotNull Context context) {
        super(context);
        this.material = Material.BARRIER;
        this.isNoResultsButton = true;

        label.setItemSprite(ItemStack.of(Material.BARRIER));
        label.setArgs(Component.text("No Results Found!"));
    }

    @Action("label")
    private void handleSelect() {
        if (isNoResultsButton) return;
        performSignal(SIG_SELECTED, material);
    }
}

package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapIconPreview extends View {
    public static final String SIG_SELECTED = "map_icon_preview.selected";

    private @Outlet("label") Label label;

    private final Material material;

    public MapIconPreview(@NotNull Context context, @NotNull Material material) {
        super(context);
        this.material = material;

        label.setItemSprite(ItemStack.of(material));
        label.setArgs(Component.text(material.name()));
    }

    @Action("label")
    private void handleSelect() {
        performSignal(SIG_SELECTED, material);
    }

}
